package org.jabref.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.l10n.Language;
import org.jabref.model.database.BibDatabaseMode;

public class GeneralPreferences {
    private final ObjectProperty<Language> language;
    private final ObjectProperty<BibDatabaseMode> defaultBibDatabaseMode;

    public GeneralPreferences(Language language,
                              BibDatabaseMode defaultBibDatabaseMode) {
        this.language = new SimpleObjectProperty<>(language);
        this.defaultBibDatabaseMode = new SimpleObjectProperty<>(defaultBibDatabaseMode);
    }

    public Language getLanguage() {
        return language.get();
    }

    public ObjectProperty<Language> languageProperty() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language.set(language);
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
