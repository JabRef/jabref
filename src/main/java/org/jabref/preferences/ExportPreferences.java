package org.jabref.preferences;

import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.model.metadata.SaveOrder;

public class ExportPreferences {

    private final StringProperty lastExportExtension;
    private final ObjectProperty<Path> exportWorkingDirectory;
    private final ObjectProperty<SaveOrder> exportSaveOrder;
    private final ObservableList<TemplateExporter> customExporters;

    public ExportPreferences(String lastExportExtension,
                             Path exportWorkingDirectory,
                             SaveOrder exportSaveOrder,
                             List<TemplateExporter> customExporters) {
        this.lastExportExtension = new SimpleStringProperty(lastExportExtension);
        this.exportWorkingDirectory = new SimpleObjectProperty<>(exportWorkingDirectory);
        this.exportSaveOrder = new SimpleObjectProperty<>(exportSaveOrder);
        this.customExporters = FXCollections.observableList(customExporters);
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

    public ObservableList<TemplateExporter> getCustomExporters() {
        return customExporters;
    }

    public void setCustomExporters(List<TemplateExporter> exporters) {
        customExporters.clear();
        customExporters.addAll(exporters);
    }
}
