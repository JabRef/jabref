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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateModifyExporterDialogViewModel extends AbstractViewModel {

    //This could be separated into two dialogs, one for adding exporters and one for modifying them.
    //See the two constructors for the view, as was done in CustomExportDialog

    //The view will return a TemplateExporter
    //You will have to look at Swing class CustomExportDialog when you write the View

    //Cancel to be implemented in View, not here

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateModifyExporterDialogViewModel.class);

    private final TemplateExporter exporter;
    private final DialogService dialogService;
    private final PreferencesService preferences;

    private final SimpleStringProperty name;
    private final SimpleStringProperty layoutFile;
    private final SimpleStringProperty extension;


    public CreateModifyExporterDialogViewModel(TemplateExporter exporter, DialogService dialogService, PreferencesService preferences,
                                               String name, String layoutFile, String extension) {
        this.exporter = exporter;
        this.dialogService = dialogService;
        this.preferences = preferences;

        //Set text of each of the boxes
        this.name = name;
        this.layoutFile = layoutFile;
        this.extension = extension;
    }

    public TemplateExporter saveExporter() {//void?
        Path layoutFileDir = Paths.get(layoutFile.get()).getParent();
        if (layoutFileDir != null) {
            String layoutFileDirString = layoutFileDir.toString();
            preferences.setExportWorkingDirectory(layoutFileDirString);

        }

        // Check that there are no empty strings.
        if (layoutFile.get().isEmpty() || name.get().isEmpty() || extension.get().isEmpty()
            || !layoutFile.get().endsWith(".layout")) {

            LOGGER.info("One of the fields is empty!"); //TODO: Better error message
            return null; //return implemented similarly to CleanupDialog, although JavaFX documentation says you need something
            //like a result converter, which must be in the view, see class EntryTypeView
        }

        // Create a new exporter to be returned to ExportCustomizationDialogViewModel, which requested it
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
