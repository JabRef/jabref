package org.jabref.gui.undo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import org.jabref.model.change.BibChange;
import org.jabref.model.change.ChangeSet;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// The undo journal: a stack of changes, and the API for putting changes on it.
///
/// Recording and undoing live on the same object on purpose. A command already has to be
/// handed the journal in order to record anything, so giving the recording API its own type
/// would mean threading a second handle everywhere the first one already goes.
///
/// Plain Java on purpose. Nothing here hops to the JavaFX thread, so recording a change works
/// in a plain unit test and the journal could in time be used outside the GUI. Menu enablement
/// subscribes through [UndoManagerProperties], which owns that hop.
///
/// Stack operations are synchronized because commands push from background tasks — cleanup and
/// import both do. [#record] is not: it runs caller code, so holding the lock across it would
/// invite deadlock, and one recording block at a time is what commands actually do.
@NullMarked
public class UndoManager {

    private final Deque<BibChange> undoStack = new ArrayDeque<>();
    private final Deque<BibChange> redoStack = new ArrayDeque<>();

    /// Notified after every change to either stack. Commands push from background tasks, so a
    /// listener that touches the UI is responsible for getting itself onto the right thread.
    private final List<Runnable> listeners = new ArrayList<>();

    /// Depth of the undo stack when the library was last saved, so that undoing back to it
    /// reports the library as unchanged again.
    private int savedDepth;

    /// Non-null exactly while a [#record] block is in progress.
    private @Nullable ChangeRecorder active;

    /// Records a single change as its own undo step, or as part of the enclosing step when
    /// called inside [#record].
    ///
    /// A lone change needs no group and therefore no name: a [ChangeSet] exists to hold
    /// several changes together, and its name describes that grouping to the user.
    public synchronized void addEdit(BibChange change) {
        if (active != null) {
            active.record(change);
            return;
        }
        undoStack.push(change);
        redoStack.clear();
        notifyListeners();
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
    /// @return whether anything was recorded, for callers that report the outcome to the user
    public boolean record(String name, Consumer<ChangeRecorder> mutations) {
        ChangeRecorder enclosing = active;
        ChangeRecorder recorder = new ChangeRecorder(name);

        active = recorder;
        try {
            mutations.accept(recorder);
        } finally {
            active = enclosing;
        }

        ChangeSet changeSet = recorder.toChangeSet();
        if (changeSet.isEmpty()) {
            return false;
        }
        if (enclosing == null) {
            addEdit(changeSet);
        } else {
            enclosing.record(changeSet);
        }
        return true;
    }

    public synchronized boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public synchronized void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        BibChange change = undoStack.pop();
        change.inverted().apply();
        redoStack.push(change);
        notifyListeners();
    }

    public synchronized void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        BibChange change = redoStack.pop();
        change.apply();
        undoStack.push(change);
        notifyListeners();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /// Marks the current position as saved.
    public synchronized void markUnchanged() {
        savedDepth = undoStack.size();
    }

    /// Whether the library differs from the last saved position. Undoing back to that position
    /// reports unchanged again, which is why this compares depth rather than counting edits.
    public synchronized boolean hasChanged() {
        return undoStack.size() != savedDepth;
    }

    public synchronized void clear() {
        undoStack.clear();
        redoStack.clear();
        savedDepth = 0;
        notifyListeners();
    }

    private void notifyListeners() {
        listeners.forEach(Runnable::run);
    }
}
