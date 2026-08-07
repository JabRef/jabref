package org.jabref.logic.exporter;

import java.nio.file.Path;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;

public class ExportPreferences {

    private static final SaveOrder DEFAULT_EXPORT_SAVE_ORDER = new SaveOrder(
            SaveOrder.OrderType.ORIGINAL,
            List.of(
                    new SaveOrder.SortCriterion(InternalField.KEY_FIELD, false),
                    new SaveOrder.SortCriterion(StandardField.AUTHOR, false),
                    new SaveOrder.SortCriterion(StandardField.TITLE, false)
            )
    );

    private final StringProperty lastExportExtension;
    private final ObjectProperty<Path> exportWorkingDirectory;
    private final ObjectProperty<SaveOrder> exportSaveOrder;
    private final ObservableList<TemplateExporter> customExporters;

    private ExportPreferences() {
        this(
                "",                                       // Last export extension
                Path.of(System.getProperty("user.home")), // Export working directory
                DEFAULT_EXPORT_SAVE_ORDER,                // Export save order
                List.of()                                 // Custom exporters
        );
    }

    public ExportPreferences(String lastExportExtension,
                             Path exportWorkingDirectory,
                             SaveOrder exportSaveOrder,
                             List<TemplateExporter> customExporters) {
        this.lastExportExtension = new SimpleStringProperty(lastExportExtension);
        this.exportWorkingDirectory = new SimpleObjectProperty<>(exportWorkingDirectory);
        this.exportSaveOrder = new SimpleObjectProperty<>(exportSaveOrder);
        this.customExporters = FXCollections.observableArrayList(customExporters);
    }

    public static ExportPreferences getDefault() {
        return new ExportPreferences();
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
        customExporters.setAll(exporters);
    }
}
