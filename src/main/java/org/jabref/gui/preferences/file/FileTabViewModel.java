package org.jabref.gui.preferences.file;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.preferences.ImportExportPreferences;
import org.jabref.preferences.PreferencesService;

public class FileTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty openLastStartupProperty = new SimpleBooleanProperty();
    private final StringProperty noWrapFilesProperty = new SimpleStringProperty("");
    private final BooleanProperty resolveStringsBibTexProperty = new SimpleBooleanProperty();
    private final BooleanProperty resolveStringsAllProperty = new SimpleBooleanProperty();
    private final StringProperty resolveStringsExceptProperty = new SimpleStringProperty("");
    private final BooleanProperty alwaysReformatBibProperty = new SimpleBooleanProperty();
    private final BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final PreferencesService preferences;
    private final ImportExportPreferences importExportPreferences;

    FileTabViewModel(PreferencesService preferences) {
        this.preferences = preferences;
        this.importExportPreferences = preferences.getImportExportPreferences();
    }

    @Override
    public void setValues() {
        openLastStartupProperty.setValue(preferences.shouldOpenLastFilesOnStartup());

        noWrapFilesProperty.setValue(importExportPreferences.getNonWrappableFields());
        resolveStringsAllProperty.setValue(importExportPreferences.shouldResolveStringsForAllStrings()); // Flipped around
        resolveStringsBibTexProperty.setValue(importExportPreferences.shouldResolveStringsForStandardBibtexFields());
        resolveStringsExceptProperty.setValue(importExportPreferences.getNonResolvableFields());

        alwaysReformatBibProperty.setValue(importExportPreferences.shouldAlwaysReformatOnSave());

        autosaveLocalLibraries.setValue(preferences.shouldAutosave());
    }

    @Override
    public void storeSettings() {
        preferences.storeOpenLastFilesOnStartup(openLastStartupProperty.getValue());

        importExportPreferences.setNonWrappableFields(noWrapFilesProperty.getValue().trim());
        importExportPreferences.setResolveStringsForStandardBibtexFields(resolveStringsBibTexProperty.getValue());
        importExportPreferences.setResolveStringsForAllStrings(resolveStringsAllProperty.getValue());
        importExportPreferences.setNonResolvableFields(resolveStringsExceptProperty.getValue().trim());
        importExportPreferences.setAlwaysReformatOnSave(alwaysReformatBibProperty.getValue());

        preferences.storeShouldAutosave(autosaveLocalLibraries.getValue());
    }

    // General

    public BooleanProperty openLastStartupProperty() {
        return openLastStartupProperty;
    }

    // ImportExport

    public StringProperty noWrapFilesProperty() {
        return noWrapFilesProperty;
    }

    public BooleanProperty resolveStringsBibTexProperty() {
        return resolveStringsBibTexProperty;
    }

    public BooleanProperty resolveStringsAllProperty() {
        return resolveStringsAllProperty;
    }

    public StringProperty resolveStringsExceptProperty() {
        return resolveStringsExceptProperty;
    }

    public BooleanProperty alwaysReformatBibProperty() {
        return alwaysReformatBibProperty;
    }

    // Autosave
    public BooleanProperty autosaveLocalLibrariesProperty() {
        return autosaveLocalLibraries;
    }
}
