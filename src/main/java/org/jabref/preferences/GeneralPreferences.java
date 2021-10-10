package org.jabref.preferences;

import java.nio.charset.Charset;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.model.database.BibDatabaseMode;

public class GeneralPreferences {
    private final ObjectProperty<Charset> defaultEncoding;
    private final ObjectProperty<BibDatabaseMode> defaultBibDatabaseMode;
    private final BooleanProperty warnAboutDuplicatesInInspection;
    private final BooleanProperty confirmDelete;

    private final BooleanProperty memoryStickMode;
    private final BooleanProperty showAdvancedHints;

    public GeneralPreferences(Charset defaultEncoding,
                              BibDatabaseMode defaultBibDatabaseMode,
                              boolean warnAboutDuplicatesInInspection,
                              boolean confirmDelete,
                              boolean memoryStickMode,
                              boolean showAdvancedHints) {
        this.defaultEncoding = new SimpleObjectProperty<>(defaultEncoding);
        this.defaultBibDatabaseMode = new SimpleObjectProperty<>(defaultBibDatabaseMode);
        this.warnAboutDuplicatesInInspection = new SimpleBooleanProperty(warnAboutDuplicatesInInspection);
        this.confirmDelete = new SimpleBooleanProperty(confirmDelete);

        this.memoryStickMode = new SimpleBooleanProperty(memoryStickMode);
        this.showAdvancedHints = new SimpleBooleanProperty(showAdvancedHints);
    }

    public Charset getDefaultEncoding() {
        return defaultEncoding.get();
    }

    public ObjectProperty<Charset> defaultEncodingProperty() {
        return defaultEncoding;
    }

    public void setDefaultEncoding(Charset defaultEncoding) {
        this.defaultEncoding.set(defaultEncoding);
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

    public boolean warnAboutDuplicatesInInspection() {
        return warnAboutDuplicatesInInspection.get();
    }

    public BooleanProperty isWarnAboutDuplicatesInInspectionProperty() {
        return warnAboutDuplicatesInInspection;
    }

    public void setWarnAboutDuplicatesInInspection(boolean warnAboutDuplicatesInInspection) {
        this.warnAboutDuplicatesInInspection.set(warnAboutDuplicatesInInspection);
    }

    public boolean shouldConfirmDelete() {
        return confirmDelete.get();
    }

    public BooleanProperty confirmDeleteProperty() {
        return confirmDelete;
    }

    public void setConfirmDelete(boolean confirmDelete) {
        this.confirmDelete.set(confirmDelete);
    }

    public boolean isMemoryStickMode() {
        return memoryStickMode.get();
    }

    public BooleanProperty memoryStickModeProperty() {
        return memoryStickMode;
    }

    public void setMemoryStickMode(boolean memoryStickMode) {
        this.memoryStickMode.set(memoryStickMode);
    }

    public boolean shouldShowAdvancedHints() {
        return showAdvancedHints.get();
    }

    public BooleanProperty showAdvancedHintsProperty() {
        return showAdvancedHints;
    }

    public void setShowAdvancedHints(boolean showAdvancedHints) {
        this.showAdvancedHints.set(showAdvancedHints);
    }
}
