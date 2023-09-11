package org.jabref.gui.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.fieldeditors.WriteMetadataToSinglePdfAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

/**
 * Writes XMP Metadata to all the linked pdfs of the selected entries according to the linking entry
 */
public class WriteMetadataToLinkedPdfsAction extends SimpleCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(WriteMetadataToLinkedPdfsAction.class);

    private final StateManager stateManager;
    private final BibEntryTypesManager entryTypesManager;
    private final FieldPreferences fieldPreferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final FilePreferences filePreferences;
    private final XmpPreferences xmpPreferences;
    private final JournalAbbreviationRepository abbreviationRepository;

    public WriteMetadataToLinkedPdfsAction(DialogService dialogService,
                                           FieldPreferences fieldPreferences,
                                           FilePreferences filePreferences,
                                           XmpPreferences xmpPreferences,
                                           BibEntryTypesManager entryTypesManager,
                                           JournalAbbreviationRepository abbreviationRepository,
                                           TaskExecutor taskExecutor,
                                           StateManager stateManager) {
        this.stateManager = stateManager;
        this.entryTypesManager = entryTypesManager;
        this.fieldPreferences = fieldPreferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.filePreferences = filePreferences;
        this.xmpPreferences = xmpPreferences;
        this.abbreviationRepository = abbreviationRepository;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        BibDatabaseContext databaseContext = stateManager.getActiveDatabase().get();
        if (stateManager.getActiveDatabase().isEmpty()) {
            return;
        }

        List<BibEntry> entries = stateManager.getSelectedEntries();
        if (entries.isEmpty()) {
            entries = databaseContext.getDatabase().getEntries();

            if (entries.isEmpty()) {
                LOGGER.warn("No entry selected for fulltext download.");
                dialogService.notify(Localization.lang("This operation requires one or more entries to be selected."));
                return;
            } else {
                boolean confirm = dialogService.showConfirmationDialogAndWait(
                        Localization.lang("Write metadata to PDF files"),
                        Localization.lang("Write metadata for all PDFs in current library?"));
                if (!confirm) {
                    return;
                }
            }
        }

        dialogService.notify(Localization.lang("Writing metadata..."));

        new WriteMetaDataTask(
                databaseContext,
                entries,
                abbreviationRepository,
                entryTypesManager,
                fieldPreferences,
                filePreferences,
                xmpPreferences,
                stateManager,
                dialogService)
                .executeWith(taskExecutor);
    }

    private static class WriteMetaDataTask extends BackgroundTask<Void> {

        private final BibDatabaseContext databaseContext;
        private final List<BibEntry> entries;
        private final JournalAbbreviationRepository abbreviationRepository;
        private final BibEntryTypesManager entryTypesManager;
        private final FieldPreferences fieldPreferences;
        private final FilePreferences filePreferences;
        private final XmpPreferences xmpPreferences;
        private final StateManager stateManager;
        private final DialogService dialogService;

        private final List<Path> failedWrittenFiles = new ArrayList<>();
        private int skipped = 0;
        private int entriesChanged = 0;
        private int errors = 0;

        public WriteMetaDataTask(BibDatabaseContext databaseContext,
                                 List<BibEntry> entries,
                                 JournalAbbreviationRepository abbreviationRepository,
                                 BibEntryTypesManager entryTypesManager,
                                 FieldPreferences fieldPreferences,
                                 FilePreferences filePreferences,
                                 XmpPreferences xmpPreferences,
                                 StateManager stateManager,
                                 DialogService dialogService) {
            this.databaseContext = databaseContext;
            this.entries = entries;
            this.abbreviationRepository = abbreviationRepository;
            this.entryTypesManager = entryTypesManager;
            this.fieldPreferences = fieldPreferences;
            this.filePreferences = filePreferences;
            this.xmpPreferences = xmpPreferences;
            this.stateManager = stateManager;
            this.dialogService = dialogService;

            updateMessage(Localization.lang("Writing metadata..."));
        }

        @Override
        protected Void call() throws Exception {
            if (stateManager.getActiveDatabase().isEmpty()) {
                return null;
            }

            for (int i = 0; i < entries.size(); i++) {
                BibEntry entry = entries.get(i);
                updateProgress(i, entries.size());

                // Make a list of all PDFs linked from this entry:
                List<Path> files = entry.getFiles().stream()
                                        .map(file -> file.findIn(stateManager.getActiveDatabase().get(), filePreferences))
                                        .filter(Optional::isPresent)
                                        .map(Optional::get)
                                        .filter(FileUtil::isPDFFile)
                                        .toList();
                if (files.isEmpty()) {
                    LOGGER.debug("Skipped empty entry '{}'",
                            entry.getCitationKey().orElse(entry.getAuthorTitleYear(16)));
                    skipped++;
                } else {
                    for (Path file : files) {
                        updateMessage(Localization.lang("Writing metadata to %0", file.getFileName()));

                        if (Files.exists(file)) {
                            try {
                                WriteMetadataToSinglePdfAction.writeMetadataToFile(
                                        file,
                                        entry,
                                        databaseContext,
                                        abbreviationRepository,
                                        entryTypesManager,
                                        fieldPreferences,
                                        filePreferences,
                                        xmpPreferences);
                                entriesChanged++;
                            } catch (Exception e) {
                                LOGGER.error("Error while writing XMP data to pdf '{}'", file, e);
                                failedWrittenFiles.add(file);
                                errors++;
                            }
                        } else {
                            LOGGER.debug("Skipped non existing pdf '{}'", file);
                            skipped++;
                        }
                    }
                }
                updateMessage(Localization.lang("Processing..."));
            }

            updateMessage(Localization.lang("Finished"));
            dialogService.notify(Localization.lang("Finished writing metadata for library %0 (%1 succeeded, %2 skipped, %3 errors).",
                    databaseContext.getDatabasePath().map(Path::toString).orElse("undefined"),
                    String.valueOf(entriesChanged), String.valueOf(skipped), String.valueOf(errors)));

            if (!failedWrittenFiles.isEmpty()) {
                LOGGER.error("Failed to write XMP data to PDFs:\n" + failedWrittenFiles);
            }

            return null;
        }
    }
}
