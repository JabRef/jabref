package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javax.xml.transform.TransformerException;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes XMP metadata to the selected file according to the linking entry
 */
public class WriteMetadataToSinglePdfAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriteMetadataToSinglePdfAction.class);

    private final LinkedFile linkedFile;
    private final BibEntry entry;
    private final FieldPreferences fieldPreferences;
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final BibEntryTypesManager bibEntryTypesManager;
    private final JournalAbbreviationRepository abbreviationRepository;
    private final TaskExecutor taskExecutor;
    private final FilePreferences filePreferences;
    private final XmpPreferences xmpPreferences;

    public WriteMetadataToSinglePdfAction(LinkedFile linkedFile,
                                          BibEntry entry,
                                          BibDatabaseContext databaseContext,
                                          DialogService dialogService,
                                          FieldPreferences fieldPreferences,
                                          FilePreferences filePreferences,
                                          XmpPreferences xmpPreferences,
                                          JournalAbbreviationRepository abbreviationRepository,
                                          BibEntryTypesManager bibEntryTypesManager,
                                          TaskExecutor taskExecutor) {
        this.linkedFile = linkedFile;
        this.entry = entry;
        this.fieldPreferences = fieldPreferences;
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.bibEntryTypesManager = bibEntryTypesManager;
        this.abbreviationRepository = abbreviationRepository;
        this.taskExecutor = taskExecutor;
        this.filePreferences = filePreferences;
        this.xmpPreferences = xmpPreferences;
    }

    @Override
    public void execute() {
        BackgroundTask<Void> writeTask = BackgroundTask.wrap(() -> {
            Optional<Path> file = linkedFile.findIn(databaseContext, filePreferences);
            if (file.isEmpty()) {
                dialogService.notify(Localization.lang("Failed to write metadata, file %1 not found.", file.map(Path::toString).orElse("")));
            } else {
                    try {
                        writeMetadataToFile(file.get(), entry, databaseContext, abbreviationRepository, bibEntryTypesManager, fieldPreferences, filePreferences, xmpPreferences);
                        dialogService.notify(Localization.lang("Success! Finished writing metadata."));
                    } catch (IOException | TransformerException ex) {
                        dialogService.notify(Localization.lang("Error while writing metadata. See the error log for details."));
                        LOGGER.error("Error while writing metadata to {}", file.map(Path::toString).orElse(""), ex);
                }
            }
            return null;
        });
        writeTask
                .onRunning(() -> setExecutable(false))
                .onFinished(() -> setExecutable(true));
        taskExecutor.execute(writeTask);
    }

    public static synchronized void writeMetadataToFile(Path file,
                                                        BibEntry entry,
                                                        BibDatabaseContext databaseContext,
                                                        JournalAbbreviationRepository abbreviationRepository,
                                                        BibEntryTypesManager bibEntryTypesManager,
                                                        FieldPreferences fieldPreferences,
                                                        FilePreferences filePreferences,
                                                        XmpPreferences xmpPreferences) throws Exception {
        // Similar code can be found at {@link org.jabref.gui.exporter.WriteMetadataToPdfAction.writeMetadataToFile}
        new XmpUtilWriter(xmpPreferences).writeXmp(file, entry, databaseContext.getDatabase());

        EmbeddedBibFilePdfExporter embeddedBibExporter = new EmbeddedBibFilePdfExporter(
                databaseContext.getMode(),
                bibEntryTypesManager,
                fieldPreferences);
        embeddedBibExporter.exportToFileByPath(
                databaseContext,
                filePreferences,
                file,
                abbreviationRepository);
    }
}
