package org.jabref.gui.exporter;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.PreferencesService;

public class CreateModifyExporterDialogViewModel extends AbstractViewModel {

    //This could be separated into two dialogs, one for adding exporters and one for modifying them.
    //See the two constructors for the view

    //The view will return a TemplateExporter
    //You will have to look at Swing class CustomExportDialog when you write the View

    //Cancel to be implemented in View, not here

    private final TemplateExporter exporter;
    private final DialogService dialogService;
    private final PreferencesService preferences;


    public CreateModifyExporterDialogViewModel(TemplateExporter exporter, DialogService dialogService, PreferencesService preferences) {
        this.exporter = exporter;
        this.dialogService = dialogService;
        this.preferences = preferences;
        //Set text of each of the boxes

    }

    public TemplateExporter saveExporter() {//void?
        preferences.saveExportWorkingDirectory(layoutFileDir); //See Swing class CustomExportDialog for more on how implement this
        // Maybe create a new exporter? - see Swing version for how to do this
    }

    public String getExportWorkingDirectory() {//i.e. layout dir
        return preferences.getExportWorkingDirectory();
    }

    public void browse() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
            .addExtensionFilter(Localization.lang("Custom layout file"), StandardFileType.LAYOUT)
            .withDefaultExtension(Localization.lang("Custom layout file"), StandardFileType.LAYOUT)
            .withInitialDirectory(getExportWorkingDirectory()).build();
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(f -> setText(f.toAbsolutePath().toString())); //implement setting the text
    }

}
