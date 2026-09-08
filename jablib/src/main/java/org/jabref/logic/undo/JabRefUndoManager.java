package org.jabref.logic.undo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jabref.model.undo.ApplyResult;
import org.jabref.model.undo.BibChange;
import org.jabref.model.undo.ChangeSet;
import org.jabref.model.undo.CompoundEdit;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The undo journal: a stack of changes, and the API for putting changes on it.
///
/// Recording and undoing live on the same object on purpose: a command already holds the journal
/// in order to record, so a separate recording *object* would mean threading a second handle
/// everywhere the first one goes. A separate *interface* costs nothing, and [UndoManager] is
/// that — the recording half, which is all the ~120 classes holding a handle need. This type is
/// for the few that also drive the stacks.
///
/// Plain Java on purpose. Nothing here hops to the JavaFX thread, so recording works in a plain
/// unit test and the journal could in time be used outside the GUI. Menu enablement subscribes
/// through [org.jabref.gui.undo.GuiUndoManager], which owns that hop.
///
/// Commands push from background tasks, so each operation holds this object's monitor for
/// exactly as long as it touches the stacks:
///
///   - Applying is inside the lock, in [#applyEdit], [#undo] and [#redo] alike, or two threads
///     could undo the same change, or an undo could land between a change being applied and
///     being recorded. The price is a constraint on [BibChange#apply]: it must write to the
///     model and return, never wait for another thread — one that waited for the JavaFX thread
///     would deadlock against a menu refresh already inside [#canUndo].
///
///   - Everything else runs outside the lock: [#addEdit(String,Consumer)] never holds it while
///     calling `mutations`, and [#notifyListeners] runs after it is released, because a listener
///     may wait for the JavaFX thread.
///
/// Listeners are told *that* the stacks changed, never what changed, so they read the state they
/// need when they run. Two threads recording concurrently may therefore notify in the opposite
/// order to the one they pushed in, which does not matter: the worst case is being told twice
/// about the same state.
@NullMarked
public class JabRefUndoManager implements UndoManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefUndoManager.class);

    /// Same depth javax.swing.undo.UndoManager defaulted to. The stack retains the BibEntry
    /// objects of removed entries, so an unbounded one keeps every deletion of a session alive.
    private static final int LIMIT = 100;

    /// The id of the empty stack before anything was ever pushed. Distinct from every id
    /// [#nextId] hands out, so "nothing has been done yet" is a position like any other.
    private static final long ORIGIN = 0L;

    /// A saved position no position will ever match, so the library counts as changed until it is
    /// saved again. [#markChanged] stamps it; ids only ever grow from [#ORIGIN], so no push can
    /// reach it.
    private static final long NEVER_SAVED = -1L;

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

    /// The identity of the empty undo stack: the position the library stands at once everything
    /// still on the stack has been undone.
    ///
    /// [#ORIGIN] while nothing has been discarded. A change dropped by the [#LIMIT] trim stays
    /// applied, so from then on an empty stack means "that change, and nothing after it": it
    /// takes the id of the change that fell off, and a save taken at exactly that position is
    /// recognised again when the user undoes their way back to it. [#clear] is the other case —
    /// it discards history the library keeps, so no earlier position may match afterwards.
    private long emptyStackId = ORIGIN;

    /// The position the library was last saved at, as an id rather than a count. Compared by
    /// [#hasChanged].
    private long savedId = ORIGIN;

    /// The commands currently applying changes they have not yet handed over, in the order they
    /// started. Guarded by this object's monitor.
    ///
    /// A collection rather than a flag: two background commands can write to one library at the
    /// same time, and undo may only return once both have handed over. Ordered, so the message
    /// names the command the user has been waiting on longest — and one that has finished is no
    /// longer in it to be named.
    private final Set<Suspension> suspensions = new LinkedHashSet<>();

    /// Set while this journal is applying a change *on this thread*, in [#applyEdit], [#undo] or
    /// [#redo]. See [#isApplying] for what it is for and why a thread-local is sound here.
    private final ThreadLocal<Boolean> applying = ThreadLocal.withInitial(() -> false);

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
    /// Inside an [#addEdit] block the change still reaches the library at once while the step
    /// reaches the stack only when the block ends; the block holds the library against undo for
    /// that whole window instead.
    ///
    /// @return what was applied, and what was not — see [BibChange#apply]
    @Override
    // [impl->req~logic.undo.apply-and-record-atomically~1]
    public ApplyResult applyEdit(BibChange change) {
        CompoundEdit compound = active.get();
        if (compound != null) {
            return compound.applyEdit(change);
        }
        if (isEmptyStep(change)) {
            return ApplyResult.SUCCESS;
        }
        ApplyResult result;
        synchronized (this) {
            result = applying(change::apply);
            // A change that refused did nothing to the library, so there is nothing to take back:
            // recording it would spend the next Ctrl+Z and clear the redo stack for no reason. A
            // set that applied in part is different - what it did apply has to stay undoable.
            if (result.complete() || (change instanceof ChangeSet)) {
                push(change);
            }
        }
        notifyListeners();
        return result;
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
            // The discarded change remains applied, so the empty stack now stands for the
            // position that change produced, and inherits its id. Trimming repeatedly walks that
            // identity forward, one dropped change at a time.
            emptyStackId = undoStack.removeLast().id();
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
    /// propagates once the step is on the stack. This holds for anything thrown, an [Error]
    /// included — the library does not become less modified because the failure was severe.
    ///
    /// @return whether anything was recorded, for callers that report the outcome to the user
    @Override
    public boolean addEdit(String name, Consumer<CompoundEdit> mutations) {
        CompoundEdit enclosing = active.get();
        CompoundEdit compoundEdit = new CompoundEdit(name);

        // The block applies as it goes and pushes only at the end, so the library holds writes
        // this journal does not know about for as long as the body runs.
        UndoSuspension suspension = enclosing == null ? suspendUndo(name) : UndoSuspension.NONE;

        active.set(compoundEdit);
        try {
            mutations.accept(compoundEdit);
        } finally {
            if (enclosing == null) {
                active.remove();
            } else {
                active.set(enclosing);
            }

            try {
                // Handing over from the finally block rather than after a catch: whatever ended
                // the block — a return, a RuntimeException, an Error — the library already holds
                // what was recorded so far, and the failure travels on afterwards untouched.
                // `active` is restored first, so this lands in the enclosing block if there is one.
                ChangeSet changeSet = compoundEdit.toChangeSet();
                if (!changeSet.isEmpty()) {
                    addEdit(changeSet);
                }
            } finally {
                // After the push, so the window does not reopen between the last write and the
                // record; and in a finally, so a block that failed does not hold the library.
                suspension.close();
            }
        }
        return compoundEdit.hasEdits();
    }

    /// Suspends undo and redo for this library until the returned suspension is closed.
    ///
    /// Opening and closing are both stack changes as far as observers are concerned — enablement
    /// has to fall while a command holds the library and rise again afterwards — so both notify,
    /// and both do so after the monitor is released.
    @Override
    // [impl->req~logic.undo.writes-reserved-against-undo~1]
    public UndoSuspension suspendUndo(String name) {
        Suspension suspension = new Suspension(name);
        synchronized (this) {
            suspensions.add(suspension);
        }
        notifyListeners();
        return suspension;
    }

    /// The command currently holding this library, if one is: of those still writing, the one that
    /// started first.
    ///
    /// For the Undo and Redo actions, which have to tell "nothing to undo" from "not while this is
    /// running" — both of which make [#canUndo] false.
    public synchronized Optional<String> suspendedBy() {
        return suspensions.stream().findFirst().map(Suspension::name);
    }

    /// One command's claim on this library, ended by [#close].
    private final class Suspension implements UndoSuspension {

        private final String name;
        private boolean closed;

        private Suspension(String name) {
            this.name = name;
        }

        private String name() {
            return name;
        }

        @Override
        public void close() {
            synchronized (JabRefUndoManager.this) {
                // Idempotent: a task can finish through success, failure or cancellation, and
                // closing on each of them is easier to get right than closing on exactly one.
                if (closed) {
                    return;
                }
                closed = true;
                suspensions.remove(this);
            }
            notifyListeners();
        }
    }

    /// Whether there is a step to take back — the stack only, not whether a command is holding
    /// the library.
    ///
    /// Menu enablement binds to this, and a disabled menu item swallows its accelerator, so
    /// answering false during a suspension would leave Ctrl+Z doing nothing at all. [#undo]
    /// enforces the suspension, and [#suspendedBy] says which command to name.
    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /// Applies the inverse before moving the change across, so a change that throws stays
    /// undoable instead of vanishing from both stacks.
    ///
    /// A change that *refuses* — because the library no longer holds what it recorded — does move
    /// across: the step was consumed, retrying it would refuse again, and the caller is told
    /// through the result that not all of it applied.
    ///
    /// @return what was undone — its name for the user, and what of it could not be applied —
    ///         or empty if there was nothing to undo, or a command is holding the library (see
    ///         [#suspendUndo]; [#suspendedBy] tells the two apart). The name is taken inside the
    ///         monitor, and it is all that leaves the journal, so nothing outside reads the
    ///         stacks.
    public Optional<UndoStep> undo() {
        UndoStep step;
        synchronized (this) {
            if (!suspensions.isEmpty() || undoStack.isEmpty()) {
                return Optional.empty();
            }
            UndoJournalEntry journalEntry = undoStack.getFirst();
            step = new UndoStep(
                    BibChangeDescriber.describe(journalEntry.change()),
                    applying(() -> journalEntry.change().inverted().apply()).complete());
            undoStack.pop();
            // Moved with its id, so redoing returns to the position it came from rather than to
            // a new one that only looks the same.
            redoStack.push(journalEntry);
        }
        notifyListeners();
        return Optional.of(step);
    }

    /// @return what was redone, in the shape [#undo] returns it, or empty if there was nothing
    ///         to redo or a command is holding the library
    public Optional<UndoStep> redo() {
        UndoStep step;
        synchronized (this) {
            if (!suspensions.isEmpty() || redoStack.isEmpty()) {
                return Optional.empty();
            }
            UndoJournalEntry journalEntry = redoStack.getFirst();
            step = new UndoStep(
                    BibChangeDescriber.describe(journalEntry.change()),
                    applying(journalEntry.change()::apply).complete());
            redoStack.pop();
            undoStack.push(journalEntry);
        }
        notifyListeners();
        return Optional.of(step);
    }

    /// Registers a listener, from any thread and at any time — including from inside another
    /// listener, since [#notifyListeners] iterates a snapshot.
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /// Marks the current position as saved.
    ///
    /// Notifies, like every other move of the saved position: a listener deriving the modified
    /// marker from [#hasChanged] has to hear about the one moment the answer turns false.
    public void markUnchanged() {
        synchronized (this) {
            // Not while a command is mid-write: what it has applied is not on the stack yet, so the
            // position being stamped does not describe what was just written to disk.
            savedId = suspensions.isEmpty() ? currentPosition() : NEVER_SAVED;
        }
        notifyListeners();
    }

    /// Whether this journal is applying a change on this thread right now.
    ///
    /// For listeners that a model write reaches synchronously, on the applying thread, while
    /// [#undo] or [#redo] is between reading the stack and moving the entry across: recording
    /// anything from there clears the redo stack under the operation that is still using it.
    /// A thread-local answers exactly that question — the event is posted inside `apply`, on this
    /// thread, before it returns — and it is not a substitute for a change knowing where it came
    /// from, which is what [org.jabref.model.entry.event.EntriesEventSource] is for.
    public boolean isApplying() {
        return applying.get();
    }

    private ApplyResult applying(Supplier<ApplyResult> apply) {
        applying.set(true);
        try {
            return apply.get();
        } finally {
            applying.remove();
        }
    }

    /// Marks the library as changed by something this journal cannot take back — a migration on
    /// load, a shared-database update, a setting written without being recorded.
    ///
    /// The stack is left alone; what moves is the saved position, to one no position can equal.
    /// Undoing every step therefore no longer means "back to what was saved", which is the truth:
    /// the change is still there. Saving is what clears it.
    public void markChanged() {
        synchronized (this) {
            savedId = NEVER_SAVED;
        }
        notifyListeners();
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
    /// Ids are never reused, so a saved position discarded by a redo-stack clear can never be
    /// matched again — correct, because the library can no longer be brought back to it. A
    /// position dropped by the [#LIMIT] trim is the opposite case: the change stays applied, so
    /// undoing everything that remains lands exactly on it, and [#emptyStackId] carries its
    /// identity for that reason.
    // [impl->req~logic.undo.saved-position-identity~1]
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
