package org.jabref.gui.exporter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.JabRefPreferences; //will be removed with writing of new method
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

    private final SimpleStringProperty name; //prevent saveExporter from saving this if it's null
    private final SimpleStringProperty layoutFile;
    private final SimpleStringProperty extension;


    public CreateModifyExporterDialogViewModel(TemplateExporter exporter, DialogService dialogService, PreferencesService preferences) {
        this.exporter = exporter;
        this.dialogService = dialogService;
        this.preferences = preferences;
        //Set text of each of the boxes

    }

    public TemplateExporter saveExporter() {//void?
        Path layoutFileDir = Paths.get(layoutFile.get()).getParent();
        if (layoutFileDir != null) {
            preferences.put(JabRefPreferences.EXPORT_WORKING_DIRECTORY, layoutFileDir.toString()); //fix to work with PreferencesService

        }
        preferences.saveExportWorkingDirectory(layoutFileDir.toString()); //See Swing class CustomExportDialog for more on how implement this
        // Create a new exporter to be returned to ExportCustomizationDialog, which requested it
        LayoutFormatterPreferences layoutPreferences = preferences.getLayoutFormatterPreferences();
        SavePreferences savePreferences = preferences.LoadForExportFromPreferences();
        String filename = layoutFile.get(); //change var name?
        String extensionString = extension.get(); //change var name?
        String lfFileName;
        //You might want to move the next few lines into logic because it also appears in JabRefPreferences
        if (filename.endsWith(".layout")) {
            lfFileName = filename.substring(0, filename.length() - ".layout".length());
        } else {
            lfFileName = filename;
        }
        if (extensionString.contains(".")) {
            extension.set(extensionString.substring(extensionString.indexOf('.') + 1, extensionString.length()));
        }
        FileType fileType = StandardFileType.newFileType(extensionString);
        TemplateExporter format = new TemplateExporter(name.get(), name.get(), lfFileName, null, fileType, layoutPreferences,
                                                       savePreferences);
        format.setCustomExport(true);
        return format;
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
