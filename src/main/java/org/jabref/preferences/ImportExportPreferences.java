package org.jabref.preferences;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ImportExportPreferences {

    private final StringProperty nonWrappableFields;
    private final BooleanProperty doNotResolveStrings;
    private final StringProperty resolvableFields;
    private final BooleanProperty alwaysReformatOnSave;
    private final ObjectProperty<Path> importWorkingDirectory;
    private final StringProperty lastExportExtension;
    private final ObjectProperty<Path> exportWorkingDirectory;

    public ImportExportPreferences(String nonWrappableFields,
                                   boolean doNotResolveStrings,
                                   String resolvableFields,
                                   boolean alwaysReformatOnSave,
                                   Path importWorkingDirectory,
                                   String lastExportExtension,
                                   Path exportWorkingDirectory) {
        this.nonWrappableFields = new SimpleStringProperty(nonWrappableFields);
        this.doNotResolveStrings = new SimpleBooleanProperty(doNotResolveStrings);
        this.resolvableFields = new SimpleStringProperty(resolvableFields);
        this.alwaysReformatOnSave = new SimpleBooleanProperty(alwaysReformatOnSave);
        this.importWorkingDirectory = new SimpleObjectProperty<>(importWorkingDirectory);
        this.lastExportExtension = new SimpleStringProperty(lastExportExtension);
        this.exportWorkingDirectory = new SimpleObjectProperty<>(exportWorkingDirectory);
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

    public boolean doNotResolveStrings() {
        return doNotResolveStrings.get();
    }

    public BooleanProperty doNotResolveStringsProperty() {
        return doNotResolveStrings;
    }

    public void setDoNotResolveStrings(boolean doNotResolveStrings) {
        this.doNotResolveStrings.set(doNotResolveStrings);
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

}
