package org.jabref.gui.collab.experimental;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.model.database.BibDatabaseContext;

public class ExternalChangeDetailsViewModel {
    private final BibDatabaseContext databaseContext;
    private final BooleanProperty accepted = new SimpleBooleanProperty();

    public ExternalChangeDetailsViewModel(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
    }

    public BooleanProperty acceptedProperty() {
        return accepted;
    }

    public boolean isAccepted() {
        return accepted.get();
    }

    public void setAccepted(boolean value) {
        acceptedProperty().set(value);
    }

    public BibDatabaseContext getDatabaseContext() {
        return databaseContext;
    }
}
