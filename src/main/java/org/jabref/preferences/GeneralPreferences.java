package org.jabref.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.database.BibDatabaseMode;

public class GeneralPreferences {

    private final ObjectProperty<BibDatabaseMode> defaultBibDatabaseMode;

    public GeneralPreferences(BibDatabaseMode defaultBibDatabaseMode) {

        this.defaultBibDatabaseMode = new SimpleObjectProperty<>(defaultBibDatabaseMode);
    }

    public BibDatabaseMode getDefaultBibDatabaseMode() {
        return defaultBibDatabaseMode.get();
    }

    public ObjectProperty<BibDatabaseMode> defaultBibDatabaseModeProperty() {
        return defaultBibDatabaseMode;
    }

    public void setDefaultBibDatabaseMode(BibDatabaseMode defaultBibDatabaseMode) {
        this.defaultBibDatabaseMode.set(defaultBibDatabaseMode);
    }
}
