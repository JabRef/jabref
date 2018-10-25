package org.jabref.gui.exporter;

import java.util.List;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.preferences.CustomExportList;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportCustomizationDialogViewModel extends BaseDialog<Void> {

    //The class vars might need to be reordered

    private final SimpleListProperty<ExporterViewModel> exporters = new SimpleListProperty<>(FXCollections.observableArrayList());

    private final int size; //final?  Or you don't need this and just use a a while loop

    //Indices within which export format information is stored within JabRefPreferences
    private static final int EXPORTER_NAME_INDEX = 0;
    private static final int EXPORTER_FILENAME_INDEX = 1;
    private static final int EXPORTER_EXTENSION_INDEX = 2;

    private final PreferencesService preferences;
    private final DialogService dialogService;

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomExportList.class);

    //Other variable declarations here

    //Also write tests for all of this

    public ExportCustomizationDialogViewModel(DialogService dialogService) {
        this.dialogService = dialogService;
        init();

        //ExporterViewModel will be organized as a singular version of what now is CustomExportDialog, which
        //currently stores all the exporters in a class var.  Each ViewModel will have one exporter and associated data, and
        //the class var exporters will be a list of them
        //You will have to write properites into ExpoerterViewModel that get all the relevant information about the exporter
        //in addition to a property for the exporter itself

    }

    public void loadExporters() {
        List<TemplateExporter> exportersLogic = preferences.getCustomExportFormats(); //Var may need more descriptive name
        for (TemplateExporter exporter : exportersLogic) {
            exporters.add(new ExporterViewModel(exporter));
        }
    }

    //The following method will have to be implemented to get information from the JavaFX analogue of Swing CustomExportDialog
    public void addExporter() {
        // open add Exporter dialog, set vars as dialogResult or analogous
        exporters.add(new ExporterViewModel(dialogResult)) //var might have to be renamed

    }

    public void saveToPrefs() {
        List<TemplateExporter> exportersLogic;
        exporters.forEach(exporter -> exportersLogic.add(exporter.getLogic()));
        preferences.storeNewExporters(exportersLogic);
    }
    public void init() {
        loadExporters();
    }
}
