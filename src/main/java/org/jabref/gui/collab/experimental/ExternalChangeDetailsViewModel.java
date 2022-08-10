package org.jabref.gui.collab.experimental;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.database.BibDatabaseContext;

public class ExternalChangeDetailsViewModel {
    private final BibDatabaseContext databaseContext;
    private final BooleanProperty accepted = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty();

    public ExternalChangeDetailsViewModel(BibDatabaseContext databaseContext, String name) {
        this.databaseContext = databaseContext;
        setName(name);
    }

    public ReadOnlyStringProperty nameProperty() {
        return name;
    }

    private void setName(String _name) {
        name.set(_name);
    }

    public String getName() {
        return name.get();
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
