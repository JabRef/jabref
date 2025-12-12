package org.jabref.gui.pseudonymize;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.exporter.AtomicFileWriter;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.pseudonymization.Pseudonymization;
import org.jabref.logic.pseudonymization.PseudonymizationResultCsvWriter;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PseudonymizeAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(PseudonymizeAction.class);
    private static final String PSEUDO_SUFFIX = ".pseudo";
    private static final String BIB_EXTENSION = ".bib";
    private static final String CSV_EXTENSION = ".csv";

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final CliPreferences preferences;

    public PseudonymizeAction(StateManager stateManager, DialogService dialogService, CliPreferences preferences) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.preferences = preferences;
        executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        dialogService.showDirectorySelectionDialog(new DirectoryDialogConfiguration.Builder().build())
                     .ifPresent(this::pseudonymizeDatabase);
    }

    private void pseudonymizeDatabase(Path outputFilePath) {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new IllegalStateException("No active database found."));
        Path inputFile = database.getDatabasePath().orElseThrow(() -> new IllegalStateException("Database has no file path"));
        Pseudonymization.Result result = new Pseudonymization().pseudonymizeLibrary(database);

        String fileName = FileUtil.getBaseName(inputFile) + PSEUDO_SUFFIX;
        Path pseudoBibPath = outputFilePath.resolve(fileName + BIB_EXTENSION);
        Path pseudoCsvPath = outputFilePath.resolve(fileName + CSV_EXTENSION);

        if (!confirmOverride(pseudoBibPath, pseudoCsvPath, outputFilePath)) {
            dialogService.notify(Localization.lang("Pseudonymization cancelled."));
            return;
        }

        try {
            exportPseudonymBibFile(pseudoBibPath, result);
            exportPseudonymKeyCsv(pseudoCsvPath, result);
            dialogService.notify(Localization.lang("%0 and %1 created at %2", pseudoBibPath.getFileName(), pseudoCsvPath.getFileName(), outputFilePath));
        } catch (IOException e) {
            dialogService.notify(Localization.lang("Unable to save files. Error occurred."));
            LOGGER.error("Error while saving file", e);
        }
    }

    private boolean confirmOverride(Path pseudoBibPath, Path pseudoCsvPath, Path outputFilePath) {
        if (Files.exists(pseudoBibPath) || Files.exists(pseudoCsvPath)) {
            return dialogService.showConfirmationDialogAndWait(Localization.lang("Pseudonymization"),
                    Localization.lang("File(s) with same name(s) found at %0. Override?", outputFilePath));
        }
        return true;
    }

    private void exportPseudonymBibFile(Path pseudoBibPath, Pseudonymization.Result result) throws IOException {
        if (!FileUtil.isBibFile(pseudoBibPath)) {
            LOGGER.error("Invalid output file type provided.");
        }
        try (AtomicFileWriter fileWriter = new AtomicFileWriter(pseudoBibPath, StandardCharsets.UTF_8)) {
            BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                    fileWriter,
                    result.bibDatabaseContext(),
                    preferences);
            databaseWriter.writeDatabase(result.bibDatabaseContext());

            // Show just a warning message if encoding did not work for all characters:
            if (fileWriter.hasEncodingProblems()) {
                LOGGER.warn("Warning: UTF-8 could not be used to encode the following characters: " + fileWriter.getEncodingProblems());
            }
            LOGGER.info("Saved " + pseudoBibPath);
        }
    }

    private void exportPseudonymKeyCsv(Path pseudoCsvPath, Pseudonymization.Result result) throws IOException {
        PseudonymizationResultCsvWriter.writeValuesMappingAsCsv(pseudoCsvPath, result);
        LOGGER.info("Saved " + pseudoCsvPath);
    }
}
