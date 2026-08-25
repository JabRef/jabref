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
/// Recording and undoing live on the same object on purpose. A command already has to be handed
/// the journal in order to record anything, so giving the recording API its own *object* would
/// mean threading a second handle everywhere the first one already goes. Its own *interface*
/// costs nothing, and [UndoManager] is that: the recording half, which is all that the ~120
/// classes holding a handle need to be able to do. This type is for the few that also drive the
/// stacks.
///
/// Plain Java on purpose. Nothing here hops to the JavaFX thread, so recording a change works
/// in a plain unit test and the journal could in time be used outside the GUI. Menu enablement
/// subscribes through [org.jabref.gui.undo.GuiUndoManager], which owns that hop.
///
/// Commands push from background tasks — cleanup and import both do — so each operation takes
/// this object's monitor for exactly as long as it touches the stacks, and no longer:
///
///   - Applying the change is inside the lock, in [#applyEdit], [#undo] and [#redo] alike.
///     It has
///     to be: if the stack transition and the model write could interleave, two threads could
///     undo the same change, or an undo could land between a change being applied and being
///     recorded.
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
public class JabRefUndoManager implements UndoManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefUndoManager.class);

    /// Same depth javax.swing.undo.UndoManager defaulted to. The stack retains the BibEntry
    /// objects of removed entries, so an unbounded one keeps every deletion of a session alive.
    private static final int LIMIT = 100;

    /// The id of the empty stack before anything was ever pushed. Distinct from every id
    /// [#nextId] hands out, so "nothing has been done yet" is a position like any other.
    private static final long ORIGIN = 0L;

    /// A change together with the identity of the position it occupies in the history.
    ///
    /// The id lives here rather than on the change because a [BibChange] is a value describing a
    /// modification, and a position in this journal is bookkeeping that only this class needs.
    /// Putting it on the change would also cost `inverted()` its involution: it would have to
    /// either copy the id, giving two distinct positions the same identity, or drop it.
    private record UndoJournalEntry(long id, BibChange change) {
    }

    private final Deque<UndoJournalEntry> undoStack = new ArrayDeque<>();
    private final Deque<UndoJournalEntry> redoStack = new ArrayDeque<>();

    /// Notified after every change to either stack. Commands push from background tasks, so a
    /// listener that touches the UI is responsible for getting itself onto the right thread.
    ///
    /// Copy-on-write rather than a plain list under the lock: registration happens a handful of
    /// times while the window is built, notification happens on every edit. Paying for a copy
    /// on the rare operation keeps the frequent one lock-free, and publishing through this list
    /// is what makes a listener registered on one thread visible to a push on another.
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    /// Source of position ids. Only ever increments, so an id identifies one position for the
    /// lifetime of this manager: a position that history has discarded can never be matched by a
    /// later one that happens to sit at the same depth.
    private long nextId = ORIGIN + 1;

    /// The identity of the empty undo stack. [#ORIGIN] until a [#LIMIT] trim or a [#clear]
    /// discards history for good — from then on an empty stack is no longer the state the
    /// library started in, so it takes a fresh id that nothing saved earlier can match.
    private long emptyStackId = ORIGIN;

    /// The position the library was last saved at, as an id rather than a count. Compared by
    /// [#hasChanged].
    private long savedId = ORIGIN;

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
    @Override
    public void addEdit(BibChange change) {
        CompoundEdit compound = active.get();
        if (compound != null) {
            compound.addEdit(change);
            return;
        }
        if (isEmptyStep(change)) {
            return;
        }
        synchronized (this) {
            push(change);
        }
        notifyListeners();
    }

    /// Performs `change` and records it in one go, so that the write to the library and the
    /// undo step describing it cannot drift apart — neither at the call site, which cannot do
    /// one without the other, nor in time, since both happen under one acquisition of the
    /// monitor.
    ///
    /// The counterpart to [#addEdit(BibChange)], and the difference between the two is only who
    /// performs the change: `addEdit` takes a change the caller has already made, this one makes
    /// it. Everything after that is the same journal entry.
    ///
    /// The single acquisition is the point. Applying and then recording as two operations leaves
    /// a window in which the library holds the change and the journal does not: an [#undo]
    /// arriving there reverts the *previous* change, and the history that ends up on the stack
    /// describes a library state that never existed. `addEdit` cannot offer this — by the time
    /// it hears about the change, the caller made it long ago.
    ///
    /// Inside an [#addEdit] block there is no window to close. That recorder belongs to one
    /// thread and nothing reaches the stacks until the block ends, so no lock is taken here.
    @Override
    public void applyEdit(BibChange change) {
        CompoundEdit compound = active.get();
        if (compound != null) {
            compound.applyEdit(change);
            return;
        }
        if (isEmptyStep(change)) {
            return;
        }
        synchronized (this) {
            change.apply();
            push(change);
        }
        notifyListeners();
    }

    /// Whether `change` would be an undo step that does nothing.
    ///
    /// A set that collected nothing is not an undo step: pushing one would enable Undo and let
    /// the next Ctrl+Z consume a no-op instead of the user's previous edit. Applying one changes
    /// nothing either, so both entry points skip it for the same reason.
    private static boolean isEmptyStep(BibChange change) {
        return (change instanceof ChangeSet changeSet) && changeSet.isEmpty();
    }

    /// Puts `change` on the undo stack as a new position and discards the redo stack, which the
    /// new change has made unreachable.
    ///
    /// Callers hold this object's monitor: the stack transition is what the monitor protects,
    /// and [#applyEdit] needs the model write to happen inside the same acquisition.
    private void push(BibChange change) {
        assert Thread.holdsLock(this);

        undoStack.push(new UndoJournalEntry(nextId++, change));
        if (undoStack.size() > LIMIT) {
            // The discarded edit remains applied to the library, so the stack it leaves
            // behind can never again be the position the library started in.
            undoStack.removeLast();
            emptyStackId = nextId++;
        }
        redoStack.clear();
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
    @Override
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
            if (undoStack.isEmpty()) {
                return;
            }
            UndoJournalEntry journalEntry = undoStack.getFirst();
            journalEntry.change().inverted().apply();
            undoStack.pop();
            // Moved with its id, so redoing returns to the position it came from rather than to
            // a new one that only looks the same.
            redoStack.push(journalEntry);
        }
        notifyListeners();
    }

    public void redo() {
        synchronized (this) {
            if (redoStack.isEmpty()) {
                return;
            }
            UndoJournalEntry journalEntry = redoStack.getFirst();
            journalEntry.change().apply();
            redoStack.pop();
            undoStack.push(journalEntry);
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
        savedId = currentPosition();
    }

    /// Whether the library differs from the last saved position.
    ///
    /// Compares *which* position the history is at, not how far it has travelled. A count cannot
    /// answer this: pushing a change clears the redo stack, so two different histories reach the
    /// same count — save, undo, then make a different edit, and an edit balance is back where it
    /// started although the library now holds something that was never saved. That reported a
    /// modified library as saved, and [org.jabref.gui.LibraryTab#requestClose] closes such a
    /// library without offering to save it.
    ///
    /// Ids are never reused, so a saved position that a redo-stack clear or a [#LIMIT] trim has
    /// discarded can never be matched again — which is the correct answer in both cases: that
    /// position is no longer reachable.
    public synchronized boolean hasChanged() {
        return currentPosition() != savedId;
    }

    /// The identity of the position the history is at: the change on top of the undo stack, or
    /// [#emptyStackId] when everything has been undone.
    private synchronized long currentPosition() {
        return undoStack.isEmpty() ? emptyStackId : undoStack.getFirst().id();
    }

    public void clear() {
        synchronized (this) {
            undoStack.clear();
            redoStack.clear();
            // A fresh id rather than ORIGIN: the discarded history is unreachable, so no position
            // saved before this point may ever compare equal again. Saving it as the current
            // position keeps a cleared manager reporting an unchanged library, as before.
            emptyStackId = nextId++;
            savedId = emptyStackId;
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
