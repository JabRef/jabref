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
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.preferences.PreferencesService;

public class CustomExporterTabViewModel implements PreferenceTabViewModel {

    private final ListProperty<ExporterViewModel> exporters = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ExporterViewModel> selectedExporters = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final JournalAbbreviationRepository repository;

    public CustomExporterTabViewModel(PreferencesService preferences, DialogService dialogService, JournalAbbreviationRepository repository) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.repository = repository;
    }

    @Override
    public void setValues() {
        List<TemplateExporter> exportersLogic = preferences.getCustomExportFormats(repository);
        for (TemplateExporter exporter : exportersLogic) {
            exporters.add(new ExporterViewModel(exporter));
        }
    }

    @Override
    public void storeSettings() {
        List<TemplateExporter> exportersLogic = exporters.stream()
                                                         .map(ExporterViewModel::getLogic)
                                                         .collect(Collectors.toList());
        preferences.storeCustomExportFormats(exportersLogic);
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

        ExporterViewModel exporterToModify = selectedExporters.get(0);
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
