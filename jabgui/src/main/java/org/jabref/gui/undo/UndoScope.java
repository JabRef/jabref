package org.jabref.gui.undo;

import java.util.function.Consumer;

import javax.swing.undo.UndoManager;

import org.jabref.model.change.BibChange;
import org.jabref.model.change.ChangeSet;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// The single seam between commands (which express user intent) and the undo journal (which
/// records model deltas).
///
/// A command performs its mutations inside [#record] and hands each resulting change to the
/// recorder. Building the [ChangeSet], discarding it when empty, and pushing it are no longer
/// the command's business, so they cannot be forgotten — which is how keyword management ended
/// up silently un-undoable before.
///
/// Not thread-safe: a scope tracks the in-progress recording in a field, so all `record` calls
/// for one library must happen on the same thread. Commands that mutate from a background task
/// already have to marshal their model writes anyway.
@NullMarked
public class UndoScope {

    private final UndoManager undoManager;

    /// Non-null exactly while a `record` call is in progress.
    private @Nullable ChangeRecorder active;

    public UndoScope(UndoManager undoManager) {
        this.undoManager = undoManager;
    }

    /// Records a single change as its own undo step, or as part of the enclosing step when
    /// called inside [#record].
    ///
    /// A lone change needs no group and therefore no name: [ChangeSet] exists to hold several
    /// changes together, and a name describes that grouping to the user.
    public void push(BibChange change) {
        if (active == null) {
            undoManager.addEdit(new BibChangeEdit(change));
        } else {
            active.record(change);
        }
    }

    /// Runs `mutations`, recording whatever it reports, and pushes the result as one undo step
    /// named `name`.
    ///
    /// Nothing is pushed if no change was recorded, so callers need not check first.
    ///
    /// A nested call becomes a nested [ChangeSet] inside its caller's set rather than a second
    /// undo step: one user action stays one undo step even when a command delegates to another.
    public void record(String name, Consumer<ChangeRecorder> mutations) {
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
            return;
        }
        if (enclosing == null) {
            undoManager.addEdit(new BibChangeEdit(changeSet));
        } else {
            enclosing.record(changeSet);
        }
    }
}
