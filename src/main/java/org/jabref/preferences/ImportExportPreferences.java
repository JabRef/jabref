package org.jabref.preferences;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ImportExportPreferences {
    private final BooleanProperty shouldOpenLastEdited;
    private final StringProperty nonWrappableFields;
    private final BooleanProperty resolveStrings;
    private final StringProperty resolvableFields;
    private final BooleanProperty alwaysReformatOnSave;
    private final ObjectProperty<Path> importWorkingDirectory;
    private final StringProperty lastExportExtension;
    private final ObjectProperty<Path> exportWorkingDirectory;
    private final BooleanProperty autoSave;
    private final BooleanProperty warnAboutDuplicatesOnImport;

    public ImportExportPreferences(boolean shouldOpenLastEdited,
                                   String nonWrappableFields,
                                   boolean resolveStrings,
                                   String resolvableFields,
                                   boolean alwaysReformatOnSave,
                                   Path importWorkingDirectory,
                                   String lastExportExtension,
                                   Path exportWorkingDirectory,
                                   boolean autoSave,
                                   boolean warnAboutDuplicatesOnImport) {
        this.shouldOpenLastEdited = new SimpleBooleanProperty(shouldOpenLastEdited);
        this.nonWrappableFields = new SimpleStringProperty(nonWrappableFields);
        this.resolveStrings = new SimpleBooleanProperty(resolveStrings);
        this.resolvableFields = new SimpleStringProperty(resolvableFields);
        this.alwaysReformatOnSave = new SimpleBooleanProperty(alwaysReformatOnSave);
        this.importWorkingDirectory = new SimpleObjectProperty<>(importWorkingDirectory);
        this.lastExportExtension = new SimpleStringProperty(lastExportExtension);
        this.exportWorkingDirectory = new SimpleObjectProperty<>(exportWorkingDirectory);
        this.autoSave = new SimpleBooleanProperty(autoSave);
        this.warnAboutDuplicatesOnImport = new SimpleBooleanProperty(warnAboutDuplicatesOnImport);
    }

    public boolean shouldOpenLastEdited() {
        return shouldOpenLastEdited.get();
    }

    public BooleanProperty openLastEditedProperty() {
        return shouldOpenLastEdited;
    }

    public void setOpenLastEdited(boolean shouldOpenLastEdited) {
        this.shouldOpenLastEdited.set(shouldOpenLastEdited);
    }

    public String getNonWrappableFields() {
        return nonWrappableFields.get();
    }

    public StringProperty nonWrappableFieldsProperty() {
        return nonWrappableFields;
    }

    public void setNonWrappableFields(String nonWrappableFields) {
        this.nonWrappableFields.set(nonWrappableFields);
    }

    public boolean resolveStrings() {
        return resolveStrings.get();
    }

    public BooleanProperty resolveStringsProperty() {
        return resolveStrings;
    }

    public void setResolveStrings(boolean resolveStrings) {
        this.resolveStrings.set(resolveStrings);
    }

    public String getResolvableFields() {
        return resolvableFields.get();
    }

    public StringProperty resolvableFieldsProperty() {
        return resolvableFields;
    }

    public void setResolvableFields(String resolvableFields) {
        this.resolvableFields.set(resolvableFields);
    }

    public boolean shouldAlwaysReformatOnSave() {
        return alwaysReformatOnSave.get();
    }

    public BooleanProperty alwaysReformatOnSaveProperty() {
        return alwaysReformatOnSave;
    }

    public void setAlwaysReformatOnSave(boolean alwaysReformatOnSave) {
        this.alwaysReformatOnSave.set(alwaysReformatOnSave);
    }

    public Path getImportWorkingDirectory() {
        return importWorkingDirectory.get();
    }

    public ObjectProperty<Path> importWorkingDirectoryProperty() {
        return importWorkingDirectory;
    }

    public void setImportWorkingDirectory(Path importWorkingDirectory) {
        this.importWorkingDirectory.set(importWorkingDirectory);
    }

    public String getLastExportExtension() {
        return lastExportExtension.get();
    }

    public StringProperty lastExportExtensionProperty() {
        return lastExportExtension;
    }

    public void setLastExportExtension(String lastExportExtension) {
        this.lastExportExtension.set(lastExportExtension);
    }

    public Path getExportWorkingDirectory() {
        return exportWorkingDirectory.get();
    }

    public ObjectProperty<Path> exportWorkingDirectoryProperty() {
        return exportWorkingDirectory;
    }

    public void setExportWorkingDirectory(Path exportWorkingDirectory) {
        this.exportWorkingDirectory.set(exportWorkingDirectory);
    }

    public boolean shouldAutoSave() {
        return autoSave.get();
    }

    public BooleanProperty autoSaveProperty() {
        return autoSave;
    }

    public void setAutoSave(boolean shouldAutoSave) {
        this.autoSave.set(shouldAutoSave);
    }

    public boolean shouldWarnAboutDuplicatesOnImport() {
        return warnAboutDuplicatesOnImport.get();
    }

    public BooleanProperty warnAboutDuplicatesOnImportProperty() {
        return warnAboutDuplicatesOnImport;
    }

    public void setWarnAboutDuplicatesOnImport(boolean warnAboutDuplicatesOnImport) {
        this.warnAboutDuplicatesOnImport.set(warnAboutDuplicatesOnImport);
    }
}
