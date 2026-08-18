package org.jabref.gui.undo;

import org.jabref.model.change.ChangeSet;
import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;

/// Adapts a [ChangeSet] to the Swing undo stack still in use.
///
/// This exists so migrated and unmigrated commands can share one stack during the migration:
/// changes recorded through [UndoScope] interleave correctly with the remaining
/// `AbstractUndoableJabRefEdit` subclasses. It disappears together with `javax.swing.undo`.
@NullMarked
public class ChangeSetEdit extends AbstractUndoableJabRefEdit {

    private final ChangeSet changeSet;
    private final BibDatabaseContext context;

    public ChangeSetEdit(ChangeSet changeSet, BibDatabaseContext context) {
        this.changeSet = changeSet;
        this.context = context;
    }

    @Override
    public void undo() {
        super.undo();
        changeSet.inverted().applyTo(context);
    }

    @Override
    public void redo() {
        super.redo();
        changeSet.applyTo(context);
    }

    @Override
    public String getPresentationName() {
        return changeSet.name();
    }
}
