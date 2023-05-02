package org.jabref.preferences;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.metadata.SaveOrder;

public class ImportExportPreferences {
    private final BooleanProperty alwaysReformatOnSave;
    private final ObjectProperty<Path> importWorkingDirectory;
    private final StringProperty lastExportExtension;
    private final ObjectProperty<Path> exportWorkingDirectory;
    private final ObjectProperty<SaveOrder> exportSaveOrder;
    private final BooleanProperty autoSave;
    private final BooleanProperty warnAboutDuplicatesOnImport;

    public ImportExportPreferences(boolean alwaysReformatOnSave,
                                   Path importWorkingDirectory,
                                   String lastExportExtension,
                                   Path exportWorkingDirectory,
                                   SaveOrder exportSaveOrder,
                                   boolean autoSave,
                                   boolean warnAboutDuplicatesOnImport) {
        this.alwaysReformatOnSave = new SimpleBooleanProperty(alwaysReformatOnSave);
        this.importWorkingDirectory = new SimpleObjectProperty<>(importWorkingDirectory);
        this.lastExportExtension = new SimpleStringProperty(lastExportExtension);
        this.exportWorkingDirectory = new SimpleObjectProperty<>(exportWorkingDirectory);
        this.exportSaveOrder = new SimpleObjectProperty<>(exportSaveOrder);
        this.autoSave = new SimpleBooleanProperty(autoSave);
        this.warnAboutDuplicatesOnImport = new SimpleBooleanProperty(warnAboutDuplicatesOnImport);
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

    public SaveOrder getExportSaveOrder() {
        return exportSaveOrder.get();
    }

    public ObjectProperty<SaveOrder> exportSaveOrderProperty() {
        return exportSaveOrder;
    }

    public void setExportSaveOrder(SaveOrder exportSaveOrder) {
        this.exportSaveOrder.set(exportSaveOrder);
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
