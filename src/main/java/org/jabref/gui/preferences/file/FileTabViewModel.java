package org.jabref.gui.preferences.file;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.ImportExportPreferences;

public class FileTabViewModel implements PreferenceTabViewModel {

    private final StringProperty noWrapFilesProperty = new SimpleStringProperty("");
    private final BooleanProperty doNotResolveStringsProperty = new SimpleBooleanProperty();
    private final BooleanProperty resolveStringsProperty = new SimpleBooleanProperty();
    private final StringProperty resolveStringsForFieldsProperty = new SimpleStringProperty("");
    private final BooleanProperty alwaysReformatBibProperty = new SimpleBooleanProperty();
    private final BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final ImportExportPreferences importExportPreferences;
    private final FieldPreferences fieldPreferences;

    FileTabViewModel(ImportExportPreferences importExportPreferences, FieldPreferences fieldPreferences) {
        this.importExportPreferences = importExportPreferences;
        this.fieldPreferences = fieldPreferences;
    }

    @Override
    public void setValues() {
        noWrapFilesProperty.setValue(FieldFactory.serializeFieldsList(fieldPreferences.getNonWrappableFields()));

        doNotResolveStringsProperty.setValue(!fieldPreferences.shouldResolveStrings());
        resolveStringsProperty.setValue(fieldPreferences.shouldResolveStrings());
        resolveStringsForFieldsProperty.setValue(FieldFactory.serializeFieldsList(fieldPreferences.getResolvableFields()));
        alwaysReformatBibProperty.setValue(importExportPreferences.shouldAlwaysReformatOnSave());
        autosaveLocalLibraries.setValue(importExportPreferences.shouldAutoSave());
    }

    @Override
    public void storeSettings() {
        fieldPreferences.setResolveStrings(!doNotResolveStringsProperty.getValue());
        fieldPreferences.setNonWrappableFields(FieldFactory.parseFieldList(noWrapFilesProperty.getValue().trim()));
        fieldPreferences.setResolvableFields(FieldFactory.parseFieldList(resolveStringsForFieldsProperty.getValue().trim()));
        importExportPreferences.setAlwaysReformatOnSave(alwaysReformatBibProperty.getValue());
        importExportPreferences.setAutoSave(autosaveLocalLibraries.getValue());
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

    // Autosave
    public BooleanProperty autosaveLocalLibrariesProperty() {
        return autosaveLocalLibraries;
    }
}
