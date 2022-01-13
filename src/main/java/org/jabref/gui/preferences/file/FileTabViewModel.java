package org.jabref.gui.preferences.file;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.preferences.ImportExportPreferences;

public class FileTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty openLastStartupProperty = new SimpleBooleanProperty();
    private final StringProperty noWrapFilesProperty = new SimpleStringProperty("");
    private final BooleanProperty resolveStringsBibTexProperty = new SimpleBooleanProperty();
    private final BooleanProperty resolveStringsAllProperty = new SimpleBooleanProperty();
    private final StringProperty resolveStringsExceptProperty = new SimpleStringProperty("");
    private final BooleanProperty alwaysReformatBibProperty = new SimpleBooleanProperty();
    private final BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final ImportExportPreferences importExportPreferences;

    FileTabViewModel(ImportExportPreferences importExportPreferences) {
        this.importExportPreferences = importExportPreferences;
    }

    @Override
    public void setValues() {
        openLastStartupProperty.setValue(importExportPreferences.shouldOpenLastEdited());
        noWrapFilesProperty.setValue(importExportPreferences.getNonWrappableFields());
        resolveStringsAllProperty.setValue(importExportPreferences.shouldResolveStringsForAllStrings()); // Flipped around
        resolveStringsBibTexProperty.setValue(importExportPreferences.shouldResolveStringsForStandardBibtexFields());
        resolveStringsExceptProperty.setValue(importExportPreferences.getNonResolvableFields());
        alwaysReformatBibProperty.setValue(importExportPreferences.shouldAlwaysReformatOnSave());
        autosaveLocalLibraries.setValue(importExportPreferences.shouldAutoSave());
    }

    @Override
    public void storeSettings() {
        importExportPreferences.setOpenLastEdited(openLastStartupProperty.getValue());
        importExportPreferences.setNonWrappableFields(noWrapFilesProperty.getValue().trim());
        importExportPreferences.setResolveStringsForStandardBibtexFields(resolveStringsBibTexProperty.getValue());
        importExportPreferences.setResolveStringsForAllStrings(resolveStringsAllProperty.getValue());
        importExportPreferences.setNonResolvableFields(resolveStringsExceptProperty.getValue().trim());
        importExportPreferences.setAlwaysReformatOnSave(alwaysReformatBibProperty.getValue());
        importExportPreferences.setAutoSave(autosaveLocalLibraries.getValue());
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
