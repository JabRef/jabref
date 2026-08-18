package org.jabref.gui.undo;

import org.jabref.model.change.BibChange;
import org.jabref.model.change.ChangeSet;

import org.jspecify.annotations.NullMarked;

/// Adapts a [BibChange] to the Swing undo stack still in use.
///
/// This exists so migrated and unmigrated commands can share one stack during the migration:
/// changes recorded through [UndoScope] interleave correctly with the remaining
/// `AbstractUndoableJabRefEdit` subclasses. It disappears together with `javax.swing.undo`.
@NullMarked
public class BibChangeEdit extends AbstractUndoableJabRefEdit {

    private final BibChange change;

    public BibChangeEdit(BibChange change) {
        this.change = change;
    }

    @Override
    public void undo() {
        super.undo();
        change.inverted().apply();
    }

    @Override
    public void redo() {
        super.redo();
        change.apply();
    }

    @Override
    public String getPresentationName() {
        // Only a change set carries a name, and nothing renders these today (see UndoScope).
        return (change instanceof ChangeSet changeSet) ? changeSet.name() : "";
    }
}
