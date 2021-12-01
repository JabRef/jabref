package org.jabref.gui.exporter;

import java.nio.file.Path;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This view model can be used both for "add exporter" and "modify exporter" functionalities.
 * It takes an optional exporter which is empty for "add exporter," and takes the selected exporter
 * for "modify exporter."  It returns an optional exporter which empty if an invalid or no exporter is
 * created, and otherwise contains the exporter to be added or that is modified.
 */

public class CreateModifyExporterDialogViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateModifyExporterDialogViewModel.class);

    private final DialogService dialogService;
    private final PreferencesService preferences;

    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty layoutFile = new SimpleStringProperty("");
    private final StringProperty extension = new SimpleStringProperty("");

    private final JournalAbbreviationRepository repository;

    public CreateModifyExporterDialogViewModel(ExporterViewModel exporter, DialogService dialogService, PreferencesService preferences,
                                               JournalAbbreviationRepository repository) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.repository = repository;

        // Set text of each of the boxes
        if (exporter != null) {
            name.setValue(exporter.name().get());
            layoutFile.setValue(exporter.layoutFileName().get());
            extension.setValue(exporter.extension().get());
        }
    }

    public ExporterViewModel saveExporter() {
        Path layoutFileDir = Path.of(layoutFile.get()).getParent();
        if (layoutFileDir != null) {
            preferences.getImportExportPreferences().setExportWorkingDirectory(layoutFileDir);
        }

        // Check that there are no empty strings.
        if (layoutFile.get().isEmpty() || name.get().isEmpty() || extension.get().isEmpty()
                || !layoutFile.get().endsWith(".layout")) {

            LOGGER.info("One of the fields is empty or invalid!");
            return null;
        }

        // Create a new exporter to be returned to ExportCustomizationDialogViewModel, which requested it
        LayoutFormatterPreferences layoutPreferences = preferences.getLayoutFormatterPreferences(repository);
        SavePreferences savePreferences = preferences.getSavePreferencesForExport();
        TemplateExporter format = new TemplateExporter(name.get(), layoutFile.get(), extension.get(),
                layoutPreferences, savePreferences);
        format.setCustomExport(true);
        return new ExporterViewModel(format);
    }

    public void browse() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(Localization.lang("Custom layout file"), StandardFileType.LAYOUT)
                .withDefaultExtension(Localization.lang("Custom layout file"), StandardFileType.LAYOUT)
                .withInitialDirectory(preferences.getImportExportPreferences().getExportWorkingDirectory()).build();
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(f -> layoutFile.set(f.toAbsolutePath().toString()));
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
