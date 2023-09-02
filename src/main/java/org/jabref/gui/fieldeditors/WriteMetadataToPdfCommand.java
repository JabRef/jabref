package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javax.xml.transform.TransformerException;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteMetadataToPdfCommand extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriteMetadataToPdfCommand.class);

    private final LinkedFile linkedFile;
    private final BibEntry entry;
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final BibEntryTypesManager bibEntryTypesManager;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final TaskExecutor taskExecutor;

    public WriteMetadataToPdfCommand(LinkedFile linkedFile,
                                     BibEntry entry,
                                     BibDatabaseContext databaseContext,
                                     DialogService dialogService,
                                     PreferencesService preferences,
                                     BibEntryTypesManager bibEntryTypesManager,
                                     JournalAbbreviationRepository abbreviationRepository,
                                     TaskExecutor taskExecutor) {
        this.linkedFile = linkedFile;
        this.entry = entry;
        this.databaseContext = databaseContext;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.bibEntryTypesManager = bibEntryTypesManager;
        this.abbreviationRepository = abbreviationRepository;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        BackgroundTask<Void> writeTask = BackgroundTask.wrap(() -> {
            Optional<Path> file = linkedFile.findIn(databaseContext, preferences.getFilePreferences());
            if (file.isEmpty()) {
                dialogService.notify(Localization.lang("Failed to write metadata, file %1 not found.", file.map(Path::toString).orElse("")));
            } else {
                synchronized (linkedFile) {
                    try {
                        // Similar code can be found at {@link org.jabref.gui.exporter.WriteMetadataToPdfAction.writeMetadataToFile}
                        new XmpUtilWriter(preferences.getXmpPreferences()).writeXmp(file.get(), entry, databaseContext.getDatabase());

                        EmbeddedBibFilePdfExporter embeddedBibExporter = new EmbeddedBibFilePdfExporter(
                                databaseContext.getMode(),
                                bibEntryTypesManager,
                                preferences.getFieldPreferences());
                        embeddedBibExporter.exportToFileByPath(
                                databaseContext,
                                preferences.getFilePreferences(),
                                file.get(),
                                abbreviationRepository);

                        dialogService.notify(Localization.lang("Success! Finished writing metadata."));
                    } catch (IOException | TransformerException ex) {
                        dialogService.notify(Localization.lang("Error while writing metadata. See the error log for details."));
                        LOGGER.error("Error while writing metadata to {}", file.map(Path::toString).orElse(""), ex);
                    }
                }
            }
            return null;
        });
        writeTask
                .onRunning(() -> setExecutable(false))
                .onFinished(() -> setExecutable(true));
        taskExecutor.execute(writeTask);
    }
}
