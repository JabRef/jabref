package org.jabref.gui.preferences.customexporter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.exporter.CreateModifyExporterDialogView;
import org.jabref.gui.exporter.ExporterViewModel;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.preferences.CliPreferences;

public class CustomExporterTabViewModel implements PreferenceTabViewModel {

    private final ListProperty<ExporterViewModel> exporters = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ExporterViewModel> selectedExporters = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final CliPreferences preferences;
    private final DialogService dialogService;

    public CustomExporterTabViewModel(CliPreferences preferences, DialogService dialogService) {
        this.preferences = preferences;
        this.dialogService = dialogService;
    }

    @Override
    public void setValues() {
        List<TemplateExporter> exportersLogic = preferences.getExportPreferences().getCustomExporters();
        exporters.clear();
        for (TemplateExporter exporter : exportersLogic) {
            exporters.add(new ExporterViewModel(exporter));
        }
    }

    @Override
    public void storeSettings() {
        List<TemplateExporter> exportersLogic = exporters.stream()
                                                         .map(ExporterViewModel::getLogic)
                                                         .collect(Collectors.toList());
        preferences.getExportPreferences().setCustomExporters(exportersLogic);
    }

    public void addExporter() {
        CreateModifyExporterDialogView dialog = new CreateModifyExporterDialogView(null);
        Optional<ExporterViewModel> exporter = dialogService.showCustomDialogAndWait(dialog);
        if ((exporter != null) && exporter.isPresent()) {
            exporters.add(exporter.get());
        }
    }

    public void modifyExporter() {
        if (selectedExporters.isEmpty()) {
            return;
        }

        ExporterViewModel exporterToModify = selectedExporters.getFirst();
        CreateModifyExporterDialogView dialog = new CreateModifyExporterDialogView(exporterToModify);
        Optional<ExporterViewModel> exporter = dialogService.showCustomDialogAndWait(dialog);
        if ((exporter != null) && exporter.isPresent()) {
            exporters.remove(exporterToModify);
            exporters.add(exporter.get());
        }
    }

    public void removeExporters() {
        exporters.removeAll(selectedExporters);
    }

    public ListProperty<ExporterViewModel> selectedExportersProperty() {
        return selectedExporters;
    }

    public ListProperty<ExporterViewModel> exportersProperty() {
        return exporters;
    }
}
