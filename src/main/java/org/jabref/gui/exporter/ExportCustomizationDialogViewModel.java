package org.jabref.gui.exporter;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.preferences.CustomExportList;
import org.jabref.preferences.JabRefPreferences;
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

        int i = 0;
        //raname var "s"?
        List<String> s;
        while (!((s = preferences.getStringList(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i)).isEmpty())) {
            //Shuold createFormat be in logic?  Check how it works in CustomExportList
            Optional<ExporterViewModel> format = createFormat(s.get(EXPORTER_NAME_INDEX), s.get(EXPORTER_FILENAME_INDEX), s.get(EXPORTER_EXTENSION_INDEX), layoutPreferences, savePreferences);
            if (format.isPresent()) {
                exporters.add(format.get()); //put was changed to add becuase we are not dealing with Map as in CustomExpoertList
            } else {
                String customExportFormat = preferences.get(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i);
                LOGGER.error("Error initializing custom export format from string " + customExportFormat);
            }
            i++;
        }
        //ExporterViewModel will be organized as a singular version of what now is CustomExportDialog, which
        //currently stores all the exporters in a class var.  Each ViewModel will have one exporter and associated data, and
        //the class var exporters will be a list of them
        //You will have to write properites into ExpoerterViewModel that get all the relevant information about the exporter
        //in addition to a property for the exporter itself

    }

    //possibly not necessary, and if so getSortedList will have to return the correct type, not Eventlist<List<String>>
    public List<ExporterViewModel> loadExporters() {
        return preferences.customExports.getSortedList(); //As of now getSortedList refers to EventList<List<String>>
    }
}
