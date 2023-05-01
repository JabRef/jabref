package org.jabref.gui.preferences.file;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.ImportExportPreferences;

public class FileTabViewModel implements PreferenceTabViewModel {

    private final StringProperty noWrapFilesProperty = new SimpleStringProperty("");
    private final BooleanProperty doNotResolveStringsProperty = new SimpleBooleanProperty();
    private final BooleanProperty resolveStringsProperty = new SimpleBooleanProperty();
    private final StringProperty resolveStringsForFieldsProperty = new SimpleStringProperty("");
    private final BooleanProperty alwaysReformatBibProperty = new SimpleBooleanProperty();
    private final BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final BooleanProperty createBackupProperty = new SimpleBooleanProperty();
    private final StringProperty backupDirectoryProperty = new SimpleStringProperty("");

    private final ImportExportPreferences importExportPreferences;
    private final FieldPreferences fieldPreferences;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;

    FileTabViewModel(ImportExportPreferences importExportPreferences, FieldPreferences fieldPreferences, FilePreferences filePreferences, DialogService dialogService) {
        this.importExportPreferences = importExportPreferences;
        this.fieldPreferences = fieldPreferences;
        this.filePreferences = filePreferences;
        this.dialogService = dialogService;
    }

    @Override
    public void setValues() {
        noWrapFilesProperty.setValue(FieldFactory.serializeFieldsList(fieldPreferences.getNonWrappableFields()));

        doNotResolveStringsProperty.setValue(!fieldPreferences.shouldResolveStrings());
        resolveStringsProperty.setValue(fieldPreferences.shouldResolveStrings());
        resolveStringsForFieldsProperty.setValue(FieldFactory.serializeFieldsList(fieldPreferences.getResolvableFields()));
        alwaysReformatBibProperty.setValue(importExportPreferences.shouldAlwaysReformatOnSave());
        autosaveLocalLibraries.setValue(importExportPreferences.shouldAutoSave());

        createBackupProperty.setValue(filePreferences.shouldCreateBackup());
        backupDirectoryProperty.setValue(filePreferences.getBackupDirectory().toString());
    }

    @Override
    public void storeSettings() {
        fieldPreferences.setResolveStrings(!doNotResolveStringsProperty.getValue());
        fieldPreferences.setNonWrappableFields(FieldFactory.parseFieldList(noWrapFilesProperty.getValue().trim()));
        fieldPreferences.setResolvableFields(FieldFactory.parseFieldList(resolveStringsForFieldsProperty.getValue().trim()));
        importExportPreferences.setAlwaysReformatOnSave(alwaysReformatBibProperty.getValue());
        importExportPreferences.setAutoSave(autosaveLocalLibraries.getValue());

        filePreferences.createBackupProperty().setValue(createBackupProperty.getValue());
        filePreferences.backupDirectoryProperty().setValue(Path.of(backupDirectoryProperty.getValue()));
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

    public BooleanProperty createBackupProperty() {
        return this.createBackupProperty;
    }

    public StringProperty backupDirectoryProperty() {
        return this.backupDirectoryProperty;
    }

    public void backupFileDirBrowse() {
        DirectoryDialogConfiguration dirDialogConfiguration =
            new DirectoryDialogConfiguration.Builder().withInitialDirectory(Path.of(backupDirectoryProperty().getValue())).build();
    dialogService.showDirectorySelectionDialog(dirDialogConfiguration)
                 .ifPresent(dir -> backupDirectoryProperty.setValue(dir.toString()));
    }
}
