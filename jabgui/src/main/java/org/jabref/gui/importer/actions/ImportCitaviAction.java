package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.CitaviXmlImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.UpdateField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action to import Citavi XML files into a new library tab.
 */
public class ImportCitaviAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportCitaviAction.class);

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;
    private final CliPreferences preferences;
    private final TaskExecutor taskExecutor;

    public ImportCitaviAction(LibraryTabContainer tabContainer,
                              CliPreferences preferences,
                              TaskExecutor taskExecutor,
                              DialogService dialogService) {
        this.tabContainer = tabContainer;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.CITAVI)
                .withDefaultExtension(StandardFileType.CITAVI)
                .withInitialDirectory(preferences.getImporterPreferences().getImportWorkingDirectory())
                .build();

        List<Path> files = dialogService.showFileOpenDialogAndGetMultipleFiles(fileDialogConfiguration);

        if (files.isEmpty()) {
            return;
        }

        importFiles(files);

        preferences.getImporterPreferences().setImportWorkingDirectory(files.getLast().getParent());
    }

    private void importFiles(List<Path> files) {
        for (Path file : files) {
            if (!Files.exists(file)) {
                dialogService.showErrorDialogAndWait(Localization.lang("Import from Citavi"),
                        Localization.lang("File not found") + ": '" + file.getFileName() + "'.");
                return;
            }
        }

        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> doImport(files));

        task.onSuccess(parserResult -> {
            tabContainer.addTab(parserResult.getDatabaseContext(), true);
            dialogService.notify(Localization.lang("%0 entry(s) imported", parserResult.getDatabase().getEntries().size()));
        })
        .onFailure(ex -> {
            LOGGER.error("Error importing from Citavi", ex);
            dialogService.notify(Localization.lang("Error importing. See the error log for details."));
        })
        .executeWith(taskExecutor);
    }

    private ParserResult doImport(List<Path> files) throws IOException {
        CitaviXmlImporter importer = new CitaviXmlImporter();
        ParserResult result = new ParserResult();

        for (Path file : files) {
            dialogService.notify(Localization.lang("Importing in %0 format", importer.getName()) + "...");
            ParserResult fileResult = importer.importDatabase(file);

            result.getDatabaseContext().getDatabase().insertEntries(fileResult.getDatabase().getEntries());
        }

        UpdateField.setAutomaticFields(
                result.getDatabase().getEntries(),
                preferences.getOwnerPreferences(),
                preferences.getTimestampPreferences());

        return result;
    }
}
