package org.jabref.gui.fieldeditors;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.PreferencesService;
import org.slf4j.Logger;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class WriteMetadataToPdfCommand extends SimpleCommand {
    private final LinkedFile linkedFile;
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferences;
    private final DialogService dialogService;
    private final BibEntry entry;
    private final Logger logger;
    private final TaskExecutor taskExecutor;

    public WriteMetadataToPdfCommand(LinkedFile linkedFile, BibDatabaseContext databaseContext, PreferencesService preferences, DialogService dialogService, BibEntry entry, Logger logger, TaskExecutor taskExecutor) {

        this.linkedFile = linkedFile;
        this.databaseContext = databaseContext;
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.entry = entry;
        this.logger = logger;
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
                        Thread.sleep(3000);
                        // Similar code can be found at {@link org.jabref.gui.exporter.WriteMetadataToPdfAction.writeMetadataToFile}
                        XmpUtilWriter.writeXmp(file.get(), entry, databaseContext.getDatabase(), preferences.getXmpPreferences());

                        EmbeddedBibFilePdfExporter embeddedBibExporter = new EmbeddedBibFilePdfExporter(databaseContext.getMode(), Globals.entryTypesManager, preferences.getFieldWriterPreferences());
                        embeddedBibExporter.exportToFileByPath(databaseContext, databaseContext.getDatabase(), preferences.getFilePreferences(), file.get());

                        dialogService.notify(Localization.lang("Success! Finished writing metadata."));
                    } catch (IOException | TransformerException ex) {
                        dialogService.notify(Localization.lang("Error while writing metadata. See the error log for details."));
                        logger.error("Error while writing metadata to {}", file.map(Path::toString).orElse(""), ex);
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
