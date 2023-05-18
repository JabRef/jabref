package org.jabref.preferences;

import java.nio.file.Path;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.metadata.SaveOrder;

public class ExportPreferences {

    private final StringProperty lastExportExtension;
    private final ObjectProperty<Path> exportWorkingDirectory;
    private final ObjectProperty<SaveOrder> exportSaveOrder;

    public ExportPreferences(String lastExportExtension,
                             Path exportWorkingDirectory,
                             SaveOrder exportSaveOrder) {

        this.lastExportExtension = new SimpleStringProperty(lastExportExtension);
        this.exportWorkingDirectory = new SimpleObjectProperty<>(exportWorkingDirectory);
        this.exportSaveOrder = new SimpleObjectProperty<>(exportSaveOrder);
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
}
