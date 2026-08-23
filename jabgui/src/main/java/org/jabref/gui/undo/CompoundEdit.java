package org.jabref.gui.undo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.undo.BibChange;
import org.jabref.model.undo.ChangeSet;
import org.jabref.model.undo.UndoableFieldChange;

import org.jspecify.annotations.NullMarked;

/// Collects the changes of one user action so they undo as one step.
///
/// Usually obtained from [UndoManager#addEdit], which owns the recorder's lifetime and pushes
/// what it collected. Constructing one directly is for the commands that cannot use a block:
/// those that collect on a background thread and push on the JavaFX thread, or that abandon
/// the operation part-way through. Those hand [UndoManager#addEdit] the result of
/// [#toChangeSet] themselves.
///
/// The overloads taking [FieldChange] exist because that is what the model already returns
/// from `setField`, `putKeywords`, `setCitationKey` and friends, so recording a change is a
/// matter of handing the return value over rather than restating it.
@NullMarked
public class CompoundEdit {

    private final String name;
    private final List<BibChange> changes = new ArrayList<>();

    public CompoundEdit(String name) {
        this.name = name;
    }

    public void addEdit(BibChange change) {
        changes.add(change);
    }

    /// Records a field change if one happened. An empty [Optional] means the model rejected
    /// the write or the value was unchanged, and nothing is recorded.
    public void addEdit(Optional<FieldChange> change) {
        change.ifPresent(this::addEdit);
    }

    public void addEdit(FieldChange change) {
        changes.add(new UndoableFieldChange(change));
    }

    /// Folds another recorder in as a nested change, so one user action stays one undo step
    /// even when a command delegates its collecting.
    public void addEdit(CompoundEdit nested) {
        if (nested.hasEdits()) {
            changes.add(nested.toChangeSet());
        }
    }

    public void addAll(Collection<FieldChange> fieldChanges) {
        fieldChanges.forEach(this::addEdit);
    }

    public boolean hasEdits() {
        return !changes.isEmpty();
    }

    public ChangeSet toChangeSet() {
        return new ChangeSet(name, changes);
    }
}
