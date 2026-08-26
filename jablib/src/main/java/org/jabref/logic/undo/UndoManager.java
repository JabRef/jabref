package org.jabref.logic.undo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.jabref.model.undo.BibChange;
import org.jabref.model.undo.ChangeSet;
import org.jabref.model.undo.CompoundEdit;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The undo journal: a stack of changes, and the API for putting changes on it.
///
/// Recording and undoing live on the same object on purpose. A command already has to be
/// handed the journal in order to record anything, so giving the recording API its own type
/// would mean threading a second handle everywhere the first one already goes.
///
/// Plain Java on purpose. Nothing here hops to the JavaFX thread, so recording a change works
/// in a plain unit test and the journal could in time be used outside the GUI. Menu enablement
/// subscribes through [org.jabref.gui.undo.GuiUndoManager], which owns that hop.
///
/// Commands push from background tasks — cleanup and import both do — so each operation takes
/// this object's monitor for exactly as long as it touches the stacks, and no longer:
///
///   - Applying the change is inside the lock. It has to be: if the stack transition and the
///     model write could interleave, two threads could undo the same change.
///   - Running foreign code is outside it. [#addEdit(String,Consumer)] never holds the lock
///     while it calls `mutations`, and [#notifyListeners] runs after the monitor is released.
///     Code this class does not own may block on another thread — a listener refreshing a menu
///     may wait for the JavaFX thread — and a lock held across such a call is a deadlock
///     waiting for the other thread to want the journal.
///
/// Listeners are told *that* the stacks changed, never what changed, so they read the state
/// they need when they run. Two threads recording concurrently may therefore notify in the
/// opposite order to the one in which they pushed, and it does not matter: every listener
/// still reads the latest state, so the worst case is being told twice about the same one.
@NullMarked
public class UndoManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoManager.class);

    /// Same depth javax.swing.undo.UndoManager defaulted to. The stack retains the BibEntry
    /// objects of removed entries, so an unbounded one keeps every deletion of a session alive.
    private static final int LIMIT = 100;

    private final Deque<BibChange> undoStack = new ArrayDeque<>();
    private final Deque<BibChange> redoStack = new ArrayDeque<>();

    /// Notified after every change to either stack. Commands push from background tasks, so a
    /// listener that touches the UI is responsible for getting itself onto the right thread.
    ///
    /// Copy-on-write rather than a plain list under the lock: registration happens a handful of
    /// times while the window is built, notification happens on every edit. Paying for a copy
    /// on the rare operation keeps the frequent one lock-free, and publishing through this list
    /// is what makes a listener registered on one thread visible to a push on another.
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    /// Net number of applied edits: +1 per recorded or redone change, -1 per undone one. A
    /// counter rather than the stack depth because [#LIMIT] discards old edits from the bottom
    /// of the stack: those edits stay applied, so the depth they leave behind no longer says
    /// how far the library has moved from the last saved position.
    private int revision;

    /// [#revision] when the library was last saved, so that undoing back to it reports the
    /// library as unchanged again.
    private int savedRevision;

    /// Set exactly while a [#addEdit] block is in progress *on this thread*. Per-thread because
    /// there is one manager for the application and long commands record from background tasks:
    /// a shared field would fold edits the user makes meanwhile into the background command's
    /// step, and would have two threads appending to one recorder's list.
    private final ThreadLocal<@Nullable CompoundEdit> active = new ThreadLocal<>();

    /// Records a single change as its own undo step, or as part of the enclosing step when
    /// called inside [#addEdit].
    ///
    /// A lone change needs no group and therefore no name: a [ChangeSet] exists to hold
    /// several changes together, and its name describes that grouping to the user.
    public void addEdit(BibChange change) {
        CompoundEdit compound = active.get();
        if (compound != null) {
            compound.addEdit(change);
            return;
        }
        // A set that collected nothing is not an undo step. Pushing one would enable Undo and
        // let the next Ctrl+Z consume a no-op instead of the user's previous edit.
        if ((change instanceof ChangeSet changeSet) && changeSet.isEmpty()) {
            return;
        }
        synchronized (this) {
            undoStack.push(change);
            revision++;
            if (undoStack.size() > LIMIT) {
                // Only the stack is trimmed. The discarded edit remains applied to the library,
                // so it still counts towards the distance from the last saved position.
                undoStack.removeLast();
            }
            redoStack.clear();
        }
        notifyListeners();
    }

    /// Performs `change` and records it in one go, so that the write to the library and the
    /// undo step describing it cannot drift apart at the call site.
    public void apply(BibChange change) {
        change.apply();
        addEdit(change);
    }

    /// Runs `mutations`, recording whatever it reports, and pushes the result as one undo step
    /// named `name`.
    ///
    /// Nothing is pushed if no change was recorded, so callers need not check first — which is
    /// the point: a command cannot collect changes and then forget to hand them over.
    ///
    /// A nested call becomes a nested [ChangeSet] inside its caller's set rather than a second
    /// undo step, so one user action stays one undo step even when a command delegates.
    ///
    /// A block that fails part-way through still hands over what it managed to change. The
    /// library is modified either way at that point, and the difference between the two paths
    /// is only whether the user can take it back. The failure itself is not swallowed: it
    /// propagates once the step is on the stack.
    ///
    /// @return whether anything was recorded, for callers that report the outcome to the user
    public boolean addEdit(String name, Consumer<CompoundEdit> mutations) {
        CompoundEdit enclosing = active.get();
        CompoundEdit compoundEdit = new CompoundEdit(name);
        RuntimeException failure = null;

        active.set(compoundEdit);
        try {
            mutations.accept(compoundEdit);
        } catch (RuntimeException e) {
            // Rethrown only after the handover below, so a block that fails part-way through
            // still hands over what it changed.
            failure = e;
        } finally {
            if (enclosing == null) {
                active.remove();
            } else {
                active.set(enclosing);
            }
        }

        // `active` is restored, so this lands in the enclosing block if there is one.
        ChangeSet changeSet = compoundEdit.toChangeSet();
        if (!changeSet.isEmpty()) {
            addEdit(changeSet);
        }
        if (failure != null) {
            throw failure;
        }
        return !changeSet.isEmpty();
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /// Applies the inverse before moving the change across, so a change that throws stays
    /// undoable instead of vanishing from both stacks.
    public void undo() {
        synchronized (this) {
            BibChange change = undoStack.peek();
            if (change == null) {
                return;
            }
            change.inverted().apply();
            undoStack.pop();
            redoStack.push(change);
            revision--;
        }
        notifyListeners();
    }

    public void redo() {
        synchronized (this) {
            BibChange change = redoStack.peek();
            if (change == null) {
                return;
            }
            change.apply();
            redoStack.pop();
            undoStack.push(change);
            revision++;
        }
        notifyListeners();
    }

    /// Registers a listener, from any thread and at any time — including from inside another
    /// listener, since [#notifyListeners] iterates a snapshot.
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /// Marks the current position as saved.
    public synchronized void markUnchanged() {
        savedRevision = revision;
    }

    /// Whether the library differs from the last saved position. Undoing back to that position
    /// reports unchanged again, which is why this compares [#revision] rather than counting
    /// the edits still on the stack.
    public synchronized boolean hasChanged() {
        return revision != savedRevision;
    }

    public void clear() {
        synchronized (this) {
            undoStack.clear();
            redoStack.clear();
            revision = 0;
            savedRevision = 0;
        }
        notifyListeners();
    }

    /// Runs every listener even if one of them throws. By the time this is called the change is
    /// on the stack and applied to the library, so letting a listener's failure out would
    /// report a completed operation as failed — and would silently deprive the remaining
    /// listeners of a notification they have no other way to get.
    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception e) {
                LOGGER.error("Undo listener failed", e);
            }
        }
    }
}
