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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportCustomizationDialogViewModel extends AbstractViewModel {

    //The class vars might need to be reordered

    //exporters should probably be a JavaFX SortedList instead of SimpleListPreperty,
    //but not yet sure how to make SortedList into a property
    private final ListProperty<ExporterViewModel> exporters = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ListProperty<ExporterViewModel> selectedExporters = new SimpleListProperty<>(FXCollections.observableArrayList());

    //Indices within which export format information is stored within JabRefPreferences, currently unused
    private static final int EXPORTER_NAME_INDEX = 0;
    private static final int EXPORTER_FILENAME_INDEX = 1;
    private static final int EXPORTER_EXTENSION_INDEX = 2;

    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final JournalAbbreviationLoader loader;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportCustomizationDialogViewModel.class); //currently unused

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
        ExporterViewModel blankExporter = new ExporterViewModel();
        CreateModifyExporterDialogView dialog = new CreateModifyExporterDialogView(blankExporter);
        Optional<ExporterViewModel> exporter = dialogService.showCustomDialogAndWait(dialog);
        if (exporter.isPresent()) {
            exporters.add(exporter.get());
        }
    }

    public void modifyExporter() {
        // open modify Exporter dialog, which is the same as add Exporter dialog but beginning with a non-blank ExporterViewModel,
        // and set that into exporters.
        CreateModifyExporterDialogView dialog = new CreateModifyExporterDialogView(selectedExporters.get(0));
        Optional<ExporterViewModel> exporter = dialogService.showCustomDialogAndWait(dialog);
        if (exporter.isPresent()) {
            exporters.add(exporter.get()); //result must come from dialog, and this will append the exporter unless you make a sorted list property
        }
    }

    public void removeExporters() {
        if (selectedExporters.getSize() == 0) { // Is this check necessary?
            return;
        }
        for (ExporterViewModel exporter : selectedExporters) {
            exporters.remove(exporter);
        }
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
