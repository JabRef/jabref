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

    //The class vars might need to be reordered

    //exporters should probably be a JavaFX SortedList instead of SimpleListPreperty,
    //but not yet sure how to make SortedList into a property
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

        //ExporterViewModel is organized as a singular version of what now is CustomExportDialog, which
        //currently stores all the exporters in a class var.  Each ExporterViewModel wraps an exporter, and
        //the class var exporters is a list of the ExporterViewModels

    }

    public void loadExporters() {
        List<TemplateExporter> exportersLogic = preferences.getCustomExportFormats(loader); //Var exportersLogic may need more descriptive name
        for (TemplateExporter exporter : exportersLogic) {
            exporters.add(new ExporterViewModel(exporter));
        }
    }

    public void addExporter() {
        Optional<ExporterViewModel> blankExporter = Optional.empty();
        CreateModifyExporterDialogView dialog = new CreateModifyExporterDialogView(blankExporter, dialogService, preferences,
                                                                                   loader);
        Optional<Optional<ExporterViewModel>> exporter = dialogService.showCustomDialogAndWait(dialog);
        if (exporter.isPresent() && exporter.get().isPresent()) {
            exporters.add(exporter.get().get());
        }
    }

    public void modifyExporter() {
        // open modify Exporter dialog, which is the same as add Exporter dialog but beginning with a non-blank ExporterViewModel,
        // and set that into exporters.
        CreateModifyExporterDialogView dialog;
        ExporterViewModel exporterToModify = selectedExporters.get(0);
        try {
            dialog = new CreateModifyExporterDialogView(Optional.of(exporterToModify),
                                                        dialogService, preferences, loader);
        } catch (IndexOutOfBoundsException ex) {
            Optional<ExporterViewModel> emptyExporter = Optional.empty();
            dialog = new CreateModifyExporterDialogView(emptyExporter, dialogService,
                                                                                       preferences, loader);
        }
        Optional<Optional<ExporterViewModel>> exporter = dialogService.showCustomDialogAndWait(dialog);
        if (exporter.isPresent() && exporter.get().isPresent()) { //First optional because you may not have entered a exporter to begin with, and second because you may not have outputted one at the end
            exporters.remove(exporterToModify);
            exporters.add(exporter.get().get()); // this will append the exporter unless you make a sorted list property
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
