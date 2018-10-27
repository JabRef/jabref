package org.jabref.gui.exporter;

import java.util.ArrayList;
import java.util.List;

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
    private final SimpleListProperty<ExporterViewModel> exporters = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final int size; //final?  Or you don't need this and just use a a while loop

    //Indices within which export format information is stored within JabRefPreferences
    private static final int EXPORTER_NAME_INDEX = 0;
    private static final int EXPORTER_FILENAME_INDEX = 1;
    private static final int EXPORTER_EXTENSION_INDEX = 2;

    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final JournalAbbreviationLoader loader;

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomExportList.class);

    //Other variable declarations here

    //Also write tests for all of this if necessary

    public ExportCustomizationDialogViewModel(DialogService dialogService, JournalAbbreviationLoader loader) {
        this.dialogService = dialogService;
        this.loader = loader;
        init();

        //ExporterViewModel is organized as a singular version of what now is CustomExportDialog, which
        //currently stores all the exporters in a class var.  Each ExporterViewModel wraps an exporter, and
        //the class var exporters is a list of the ExporterViewModels

    }

    public void loadExporters() {
        List<TemplateExporter> exportersLogic = preferences.getCustomExportFormats(loader); //Var may need more descriptive name
        for (TemplateExporter exporter : exportersLogic) {
            exporters.add(new ExporterViewModel(exporter));
        }
    }

    //The following method will have to be implemented to get information from the JavaFX analogue of Swing CustomExportDialog
    public void addExporter() {
        // open add Exporter dialog, set vars as dialogResult or analogous
        TemplateExporter exporter = new CreateModifyExporterDialogView().show(); //Not sure if this is right
        exporters.add(new ExporterViewModel(exporter));//var might have to be renamed

    }

    public void modifyExporter(int row) {
        // open modify Exporter dialog, which may be the same as add Exporter dialog, and set that into exporters.
        exporters.set(row, new ExporterViewModel(dialogResult)); //result must come from dialog
    }

    public void removeExporters(int[] rows) {
        if (rows.length == 0) { // Is this check necessary?  Probably not
            return;
        }
        for (int i = 0; i < rows.length; i++) {
            exporters.remove(rows[i]);
        }
    }

    public void saveToPrefs() {
        List<TemplateExporter> exportersLogic;
        exporters.forEach(exporter -> exportersLogic.add(exporter.getLogic()));
        preferences.storeCustomExportFormats(exportersLogic);
    }
    public void init() {
        loadExporters();
    }
}
