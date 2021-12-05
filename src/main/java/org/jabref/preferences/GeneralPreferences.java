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
    private final BooleanProperty confirmEmptyEntries;
    private final BooleanProperty deleteEmptyEntries;

    private final BooleanProperty memoryStickMode;
    private final BooleanProperty showAdvancedHints;

    public GeneralPreferences(Charset defaultEncoding,
                              BibDatabaseMode defaultBibDatabaseMode,
                              boolean warnAboutDuplicatesInInspection,
                              boolean confirmDelete,
                              boolean confirmEmptyEntries,
                              boolean deleteEmptyEntries,
                              boolean memoryStickMode,
                              boolean showAdvancedHints) {
        this.defaultEncoding = new SimpleObjectProperty<>(defaultEncoding);
        this.defaultBibDatabaseMode = new SimpleObjectProperty<>(defaultBibDatabaseMode);
        this.warnAboutDuplicatesInInspection = new SimpleBooleanProperty(warnAboutDuplicatesInInspection);
        this.confirmDelete = new SimpleBooleanProperty(confirmDelete);
        this.confirmEmptyEntries = new SimpleBooleanProperty(confirmEmptyEntries);
        this.deleteEmptyEntries = new SimpleBooleanProperty(deleteEmptyEntries);

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

    /**
     * Get the preference of whether to give a confirmation dialog when empty entries are detected in the library
     *
     * @return true if the preference is set as yes
     */
    public boolean shouldConfirmEmptyEntries() {
        return confirmEmptyEntries.get();
    }

    /**
     * Get the BooleanProperty of the preference of whether to give a confirmation dialog when empty entries are detected in the library
     *
     * @return BooleanProperty
     */
    public BooleanProperty confirmEmptyEntriesProperty() {
        return confirmEmptyEntries;
    }

    /**
     * Set the preference of whether to give a confirmation dialog when empty entries are detected in the library
     *
     * @param confirmEmptyEntries        boolean for the preference
     */
    public void setConfirmEmptyEntries(boolean confirmEmptyEntries) {
        this.confirmEmptyEntries.set(confirmEmptyEntries);
    }

    /**
     * Get the preference of whether to delete the empty entries detected in the library if confirmation dialog is opted out
     *
     * @return true if the preference is set as yes (delete)
     */
    public boolean shouldDeleteEmptyEntries() {
        return deleteEmptyEntries.get();
    }

    /**
     * Get the BooleanProperty of the preference of whether to delete the empty entries detected in the library if confirmation dialog is opted out
     *
     * @return BooleanProperty
     */
    public BooleanProperty deleteEmptyEntriesProperty() {
        return deleteEmptyEntries;
    }

    /**
     * Set the preference of whether to delete the empty entries detected in the library if confirmation dialog is opted out
     *
     * @param deleteEmptyEntries        boolean for the preference
     */
    public void setDeleteEmptyEntries(boolean deleteEmptyEntries) {
        this.deleteEmptyEntries.set(deleteEmptyEntries);
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
