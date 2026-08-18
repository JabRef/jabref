package org.jabref.gui.undo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.change.BibChange;
import org.jabref.model.change.ChangeSet;
import org.jabref.model.change.FieldEdit;

import org.jspecify.annotations.NullMarked;

/// Collects the changes made inside one [UndoScope#record] call.
///
/// The overloads taking [FieldChange] exist because that is what the model already returns
/// from `setField`, `putKeywords`, `setCitationKey` and friends, so recording a change is a
/// matter of handing the return value over rather than restating it.
@NullMarked
public final class ChangeRecorder {

    private final String name;
    private final List<BibChange> changes = new ArrayList<>();

    ChangeRecorder(String name) {
        this.name = name;
    }

    public void record(BibChange change) {
        changes.add(change);
    }

    /// Records a field change if one happened. An empty [Optional] means the model rejected
    /// the write or the value was unchanged, and nothing is recorded.
    public void record(Optional<FieldChange> change) {
        change.ifPresent(this::record);
    }

    public void record(FieldChange change) {
        changes.add(new FieldEdit(change));
    }

    public void recordAll(Collection<FieldChange> fieldChanges) {
        fieldChanges.forEach(this::record);
    }

    ChangeSet toChangeSet() {
        return new ChangeSet(name, changes);
    }
}
