package org.jabref.gui.exporter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.preferences.PreferencesService;

public class ExportCustomizationDialogViewModel extends AbstractViewModel {

    private final ListProperty<ExporterViewModel> exporters = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ExporterViewModel> selectedExporters = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final JournalAbbreviationRepository repository;

    public ExportCustomizationDialogViewModel(PreferencesService preferences, DialogService dialogService, JournalAbbreviationRepository repository) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.repository = repository;
        loadExporters();
    }

    private void loadExporters() {
        List<TemplateExporter> exportersLogic = preferences.getCustomExportFormats(repository);
        for (TemplateExporter exporter : exportersLogic) {
            exporters.add(new ExporterViewModel(exporter));
        }
    }

    public void addExporter() {
        CreateModifyExporterDialogView dialog = new CreateModifyExporterDialogView(null);
        Optional<ExporterViewModel> exporter = dialogService.showCustomDialogAndWait(dialog);
        if ((exporter != null) && exporter.isPresent()) {
            exporters.add(exporter.get());
        }
    }

    public void modifyExporter() {
        CreateModifyExporterDialogView dialog;
        ExporterViewModel exporterToModify = null;
        if (selectedExporters.isEmpty()) {
            return;
        }
        exporterToModify = selectedExporters.get(0);
        dialog = new CreateModifyExporterDialogView(exporterToModify);
        Optional<ExporterViewModel> exporter = dialogService.showCustomDialogAndWait(dialog);
        if ((exporter != null) && exporter.isPresent()) {
            exporters.remove(exporterToModify);
            exporters.add(exporter.get());
        }
    }

    public void removeExporters() {
        exporters.removeAll(selectedExporters);
    }

    public void saveToPrefs() {
        List<TemplateExporter> exportersLogic = exporters.stream().map(ExporterViewModel::getLogic).collect(Collectors.toList());
        preferences.storeCustomExportFormats(exportersLogic);
    }

    public ListProperty<ExporterViewModel> selectedExportersProperty() {
        return selectedExporters;
    }

    public ListProperty<ExporterViewModel> exportersProperty() {
        return exporters;
    }
}
