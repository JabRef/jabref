package org.jabref.gui.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs an export action
 */
public class ExportCommand extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportCommand.class);
    private final JabRefFrame frame;
    private final boolean selectedOnly;

    /**
     * @param selectedOnly true if only the selected entries should be exported, otherwise all entries are exported
     */
    public ExportCommand(JabRefFrame frame, boolean selectedOnly) {
        this.frame = frame;
        this.selectedOnly = selectedOnly;
    }

    @Override
    public void execute() {
        Globals.exportFactory = ExporterFactory.create(Globals.prefs, Globals.journalAbbreviationLoader);
        FileDialogConfiguration fileDialogConfiguration = createExportFileChooser(Globals.exportFactory, Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY));
        DialogService dialogService = frame.getDialogService();
        DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showFileSaveDialog(fileDialogConfiguration)
                .ifPresent(path -> export(path, fileDialogConfiguration.getSelectedExtensionFilter(), Globals.exportFactory.getExporters())));
    }

    private void export(Path file, FileChooser.ExtensionFilter selectedExtensionFilter, List<Exporter> exporters) {
        String selectedExtension = selectedExtensionFilter.getExtensions().get(0).replace("*", "");
        if (!file.endsWith(selectedExtension)) {
            FileUtil.addExtension(file, selectedExtension);
        }

        if (Files.exists(file)) {
            // Warn that the file exists:

            boolean overwriteFilePressed = frame.getDialogService().showConfirmationDialogAndWait(Localization.lang("Export"),
                    Localization.lang("'%0' exists. Overwrite file?", file.getFileName().toString()),
                    Localization.lang("Overwrite file"),
                    Localization.lang("Cancel"));

            if (!overwriteFilePressed) {
                return;
            }
        }
        final Exporter format = FileFilterConverter.getExporter(selectedExtensionFilter, exporters).orElseThrow(() -> new IllegalStateException("User didn't selected a file type for the extension"));
        List<BibEntry> entries;
        if (selectedOnly) {
            // Selected entries
            entries = frame.getCurrentBasePanel().getSelectedEntries();
        } else {
            // All entries
            entries = frame.getCurrentBasePanel().getDatabase().getEntries();
        }

        // Set the global variable for this database's file directory before exporting,
        // so formatters can resolve linked files correctly.
        // (This is an ugly hack!)
        Globals.prefs.fileDirForDatabase = frame.getCurrentBasePanel()
                .getBibDatabaseContext()
                .getFileDirectories(Globals.prefs.getFileDirectoryPreferences());

        // Make sure we remember which filter was used, to set
        // the default for next time:
        Globals.prefs.put(JabRefPreferences.LAST_USED_EXPORT, format.getId());
        Globals.prefs.put(JabRefPreferences.EXPORT_WORKING_DIRECTORY, file.getParent().getFileName().toString());

        final List<BibEntry> finEntries = entries;
        AbstractWorker exportWorker = new AbstractWorker() {

            String errorMessage;

            @Override
            public void run() {
                try {
                    format.export(frame.getCurrentBasePanel().getBibDatabaseContext(),
                            file,
                            frame.getCurrentBasePanel()
                                    .getBibDatabaseContext()
                                    .getMetaData()
                                    .getEncoding()
                                    .orElse(Globals.prefs.getDefaultEncoding()),
                            finEntries);
                } catch (Exception ex) {
                    LOGGER.warn("Problem exporting", ex);
                    if (ex.getMessage() == null) {
                        errorMessage = ex.toString();
                    } else {
                        errorMessage = ex.getMessage();
                    }
                }
            }

            @Override
            public void update() {
                // No error message. Report success:
                if (errorMessage == null) {
                    frame.output(Localization.lang("%0 export successful", format.getDisplayName()));
                }
                // ... or show an error dialog:
                else {
                    frame.output(Localization.lang("Could not save file.") + " - " + errorMessage);
                    // Need to warn the user that saving failed!
                    frame.getDialogService().showErrorDialogAndWait(Localization.lang("Save library"), Localization.lang("Could not save file.") + "\n" + errorMessage);

                }
            }
        };

        // Run the export action in a background thread:
        exportWorker.getWorker().run();
        // Run the update method:
        exportWorker.update();
    }

    private static FileDialogConfiguration createExportFileChooser(ExporterFactory exportFactory, String currentDir) {
        List<FileType> fileTypes = exportFactory.getExporters().stream().map(Exporter::getFileType).collect(Collectors.toList());
        return new FileDialogConfiguration.Builder()
                .addExtensionFilters(fileTypes)
                .withDefaultExtension(Globals.prefs.get(JabRefPreferences.LAST_USED_EXPORT))
                .withInitialDirectory(currentDir)
                .build();
    }

}
