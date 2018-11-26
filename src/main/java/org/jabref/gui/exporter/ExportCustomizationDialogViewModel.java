package org.jabref.gui.exporter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.preferences.PreferencesService;

public class ExportCustomizationDialogViewModel extends AbstractViewModel {

    private final ListProperty<ExporterViewModel> exporters = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ExporterViewModel> selectedExporters = new SimpleListProperty<>(FXCollections.observableArrayList());


    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final JournalAbbreviationLoader loader;

    public ExportCustomizationDialogViewModel(PreferencesService preferences, DialogService dialogService, JournalAbbreviationLoader loader) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.loader = loader;
        init();
    }

    private void loadExporters() {
        List<TemplateExporter> exportersLogic = preferences.getCustomExportFormats(loader); //Var exportersLogic may need more descriptive name
        for (TemplateExporter exporter : exportersLogic) {
            exporters.add(new ExporterViewModel(exporter));
        }
    }

    public void addExporter() {
        CreateModifyExporterDialogView dialog = new CreateModifyExporterDialogView(null, dialogService, preferences,
                                                                                   loader);
        Optional<ExporterViewModel> exporter = dialogService.showCustomDialogAndWait(dialog);
        if ((exporter != null) && exporter.isPresent()) {
            exporters.add(exporter.get());
        }
    }

    /**
     * Open dialog to modify exporter, which is the same as adding ane exporter but passes in
     * a non-empty Optional of ExporterViewModel and sets the result into exporters.
     */

    public void modifyExporter() {
        CreateModifyExporterDialogView dialog;
        try {
            ExporterViewModel exporterToModify = selectedExporters.get(0);
            dialog = new CreateModifyExporterDialogView(exporterToModify, dialogService, preferences, loader);
        } catch (IndexOutOfBoundsException ex) {
            dialog = new CreateModifyExporterDialogView(null, dialogService, preferences, loader);
        }
        Optional<ExporterViewModel> exporter = dialogService.showCustomDialogAndWait(dialog);
        // Outer optional because an exporter may not have been entered to begin with,
        // and inner optional because one may not have been output by the dialog
        if ((exporter != null) && exporter.isPresent()) {
            exporters.remove(exporterToModify);
            // this will append the exporter to the final position unless a sorted list property is used, see TODO above
            exporters.add(exporter.get());
        }
    }

    public void removeExporters() {
        exporters.removeAll(selectedExporters);
    }

    public void saveToPrefs() {
        List<TemplateExporter> exportersLogic = new ArrayList<>();
        exporters.forEach(exporter -> exportersLogic.add(exporter.getLogic()));
        preferences.storeCustomExportFormats(exportersLogic);

    }

    public ListProperty<ExporterViewModel> selectedExportersProperty() {
        return selectedExporters;
    }

    public ListProperty<ExporterViewModel> exportersProperty() {
        return exporters;
    }

    public void init() {
        loadExporters();
    }
}
