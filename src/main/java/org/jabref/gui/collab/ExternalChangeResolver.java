package org.jabref.gui.collab;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;

public abstract class ExternalChangeResolver extends SimpleCommand {
    protected final BibDatabaseContext databaseContext;
    protected final NamedCompound undoEdit;
    private final BooleanProperty accepted = new SimpleBooleanProperty();

    protected ExternalChangeResolver(BibDatabaseContext databaseContext, NamedCompound undoEdit) {
        this.databaseContext = databaseContext;
        this.undoEdit = undoEdit;
    }

    public boolean isAccepted() {
        return accepted.get();
    }

    public BooleanProperty acceptedProperty() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted.set(accepted);
    }
}
