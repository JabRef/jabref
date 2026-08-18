package org.jabref.gui.undo;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.change.BibChange;
import org.jabref.model.change.ChangeSet;

import org.jspecify.annotations.NullMarked;

/// Collects the changes of one user action so they undo as one step.
///
/// Prefer [UndoScope#record]: it owns this object's whole lifecycle, so a command cannot
/// forget to push what it collected. This class remains for the call sites that still build
/// their compound by hand, and adapts the collected changes to the Swing undo stack that is
/// still in place.
@NullMarked
public class NamedCompoundEdit extends AbstractUndoableJabRefEdit {

    private final String name;
    private final List<BibChange> changes = new ArrayList<>();

    public NamedCompoundEdit(String name) {
        this.name = name;
    }

    public void addEdit(BibChange change) {
        changes.add(change);
    }

    /// Folds another compound in as a nested change, so one user action stays one undo step.
    public void addEdit(NamedCompoundEdit nested) {
        if (nested.hasEdits()) {
            changes.add(nested.toChangeSet());
        }
    }

    public boolean hasEdits() {
        return !changes.isEmpty();
    }

    public ChangeSet toChangeSet() {
        return new ChangeSet(name, changes);
    }

    @Override
    public void undo() {
        super.undo();
        toChangeSet().inverted().apply();
    }

    @Override
    public void redo() {
        super.redo();
        toChangeSet().apply();
    }

    @Override
    public String getPresentationName() {
        return name;
    }
}
