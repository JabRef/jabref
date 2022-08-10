package org.jabref.gui.collab.experimental;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeResolver;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;

public sealed abstract class ExternalChangeResolver extends SimpleCommand permits EntryChangeResolver {
    protected final BibDatabaseContext databaseContext;
    protected final NamedCompound undoEdit;
    private final BooleanProperty accepted = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty();

    protected ExternalChangeResolver(String changeName, BibDatabaseContext databaseContext, NamedCompound undoEdit) {
        this.databaseContext = databaseContext;
        this.undoEdit = undoEdit;
        this.name.set(changeName);
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

    public String getName() {
        return name.get();
    }
}
