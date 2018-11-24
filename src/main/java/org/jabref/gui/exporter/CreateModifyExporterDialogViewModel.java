package org.jabref.gui.exporter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This view model can be used both for "add exporter" and "modify exporter" functionalities.
 * It takes an optional exporter which is empty for "add exporter," and takes the selected exporter
 * for "modify exporter."  It returns an optional exporter which empty if an invalid or no exporter is
 * created, and otherwise contains the exporter to be added or that is modified.
 *
 */

public class CreateModifyExporterDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateModifyExporterDialogViewModel.class);

    private final DialogService dialogService;
    private final PreferencesService preferences;

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty layoutFile = new SimpleStringProperty("");
    private final StringProperty extension = new SimpleStringProperty("");

    private final JournalAbbreviationLoader loader;


    public CreateModifyExporterDialogViewModel(ExporterViewModel exporter, DialogService dialogService, PreferencesService preferences,
                                               JournalAbbreviationLoader loader) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.loader = loader;

        setTextFields(exporter);
    }

    public Optional<ExporterViewModel> saveExporter() {
        Path layoutFileDir = Paths.get(layoutFile.get()).getParent();
        if (layoutFileDir != null) {
            String layoutFileDirString = layoutFileDir.toString();
            preferences.setExportWorkingDirectory(layoutFileDirString);
        }

        // Check that there are no empty strings.
        if (layoutFile.get().isEmpty() || name.get().isEmpty() || extension.get().isEmpty()
            || !layoutFile.get().endsWith(".layout")) {

            LOGGER.info("One of the fields is empty or invalid!");
            // Return empty exporter to the main exporter customization dialog
            return Optional.empty();
        }

        // Create a new exporter to be returned to ExportCustomizationDialogViewModel, which requested it
        LayoutFormatterPreferences layoutPreferences = preferences.getLayoutFormatterPreferences(loader);
        SavePreferences savePreferences = preferences.loadForExportFromPreferences();
        TemplateExporter format = new TemplateExporter(name.get(), layoutFile.get(), extension.get(),
                                                       layoutPreferences, savePreferences);
        format.setCustomExport(true);
        return Optional.of(new ExporterViewModel(format));
    }

    public String getExportWorkingDirectory() { //i.e. layout dir
        return preferences.getExportWorkingDirectory();
    }

    public void browse() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
            .addExtensionFilter(Localization.lang("Custom layout file"), StandardFileType.LAYOUT)
            .withDefaultExtension(Localization.lang("Custom layout file"), StandardFileType.LAYOUT)
            .withInitialDirectory(getExportWorkingDirectory()).build();
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(f -> layoutFile.set(f.toAbsolutePath().toString())); //implement setting the text
    }

    private void setTextFields(ExporterViewModel exporter) {

        //Set text of each of the boxes
        if (exporter != null) {
            name.setValue(exporter.getName().get());
            layoutFile.setValue(exporter.getLayoutFileName().get());
            extension.setValue(exporter.getExtension().get());
        }
    }

    public StringProperty getName() {
        return name;
    }

    public StringProperty getLayoutFileName() {
        return layoutFile;
    }

    public StringProperty getExtension() {
        return extension;
    }

}
