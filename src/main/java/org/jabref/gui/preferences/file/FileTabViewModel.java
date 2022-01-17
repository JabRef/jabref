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
    private final BooleanProperty doNotResolveStringsProperty = new SimpleBooleanProperty();
    private final BooleanProperty resolveStringsProperty = new SimpleBooleanProperty();
    private final StringProperty resolveStringsForFieldsProperty = new SimpleStringProperty("");
    private final BooleanProperty alwaysReformatBibProperty = new SimpleBooleanProperty();
    private final BooleanProperty warnAboutDuplicatesOnImportProperty = new SimpleBooleanProperty();
    private final BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final ImportExportPreferences importExportPreferences;

    FileTabViewModel(ImportExportPreferences importExportPreferences) {
        this.importExportPreferences = importExportPreferences;
    }

    @Override
    public void setValues() {
        openLastStartupProperty.setValue(importExportPreferences.shouldOpenLastEdited());
        noWrapFilesProperty.setValue(importExportPreferences.getNonWrappableFields());

        doNotResolveStringsProperty.setValue(!importExportPreferences.resolveStrings());
        resolveStringsProperty.setValue(importExportPreferences.resolveStrings());
        resolveStringsForFieldsProperty.setValue(importExportPreferences.getResolvableFields());
        alwaysReformatBibProperty.setValue(importExportPreferences.shouldAlwaysReformatOnSave());
        warnAboutDuplicatesOnImportProperty.setValue(importExportPreferences.shouldWarnAboutDuplicatesOnImport());
        autosaveLocalLibraries.setValue(importExportPreferences.shouldAutoSave());
    }

    @Override
    public void storeSettings() {
        importExportPreferences.setOpenLastEdited(openLastStartupProperty.getValue());
        importExportPreferences.setResolveStrings(!doNotResolveStringsProperty.getValue());
        importExportPreferences.setNonWrappableFields(noWrapFilesProperty.getValue().trim());
        importExportPreferences.setResolvableFields(resolveStringsForFieldsProperty.getValue().trim());
        importExportPreferences.setAlwaysReformatOnSave(alwaysReformatBibProperty.getValue());
        importExportPreferences.setWarnAboutDuplicatesOnImport(warnAboutDuplicatesOnImportProperty.getValue());
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

    public BooleanProperty doNotResolveStringsProperty() {
        return doNotResolveStringsProperty;
    }

    public BooleanProperty resolveStringsProperty() {
        return resolveStringsProperty;
    }

    public StringProperty resolveStringsForFieldsProperty() {
        return resolveStringsForFieldsProperty;
    }

    public BooleanProperty alwaysReformatBibProperty() {
        return alwaysReformatBibProperty;
    }

    public BooleanProperty warnAboutDuplicatesOnImportProperty() { return warnAboutDuplicatesOnImportProperty; }

    // Autosave
    public BooleanProperty autosaveLocalLibrariesProperty() {
        return autosaveLocalLibraries;
    }
}
