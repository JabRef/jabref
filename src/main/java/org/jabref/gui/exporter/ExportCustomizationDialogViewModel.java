package org.jabref.gui.exporter;

import java.util.Optional;

import javax.inject.Inject;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreferencesService;

public class ExportCustomizationDialogViewModel extends BaseDialog<Void> {

    private final SimpleListProperty<ExporterViewModel> exporters = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final int size; //final?  Or you don't need this and just use a a while loop

    private final PreferencesService preferences;
    private final DialogService dialogService;

    //Other variable declarations here

    public ExportCustomizationDialogViewModel(DialogService dialogService) {
        this.dialogService = dialogService;

        int i = 0;
        //raname var "s"?
        while (!((s = preferences.getStringList(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i)))) {
            Optional<TemplateExporter> format = createFormat(s.get(EXPORTER_NAME_INDEX), s.get(EXPORTER_FILENAME_INDEX), s.get(EXPORTER_EXTENSION_INDEX), layoutPreferences, savePreferences);
            if (format.isPresent()) {
                exporters.add(format.get()); //put was changed to add becuase we are not dealing with Map as in CustomExpoertList
            } else {
                String customExportFormat = preferences.get(JabRefPreferences.CUSTOM_EXPORT_FORMAT + i);
                LOGGER.error("Error initializing custom export format from string " + customExportFormat);
            }
            i++;
        }
        //exporters.addAll(//add here); //How will ExporterViewModel be organized?
                         //It should be analogous to AbbreviationViewModel, except possibly more complicated
                         //Rather than use addAll(), you might want to take from CustomExportList.
                         //You may have to write a method that gets all the relevant information about the exporters

    }

    //possibly not necessary, and if so getSortedList will have to return the correct type, not Eventlist<List<String>>
    public List<ExporterViewModel> loadExporters() {
        return preferences.customExports.getSortedList(); //As of now getSortedList refers to EventList<List<String>>
    }
}