package org.jabref.gui.exporter;

import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import javafx.stage.FileChooser;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.MnemonicAwareAction;
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

public class ExportAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportAction.class);

    private ExportAction() {
    }


    /**
     * Create an AbstractAction for performing an export operation.
     *
     * @param frame
     *            The JabRefFrame of this JabRef instance.
     * @param selectedOnly
     *            true indicates that only selected entries should be exported,
     *            false indicates that all entries should be exported.
     * @return The action.
     */
    public static AbstractAction getExportAction(JabRefFrame frame, boolean selectedOnly) {

        class InternalExportAction extends MnemonicAwareAction {

            private final JabRefFrame frame;

            private final boolean selectedOnly;

            public InternalExportAction(JabRefFrame frame, boolean selectedOnly) {
                this.frame = frame;
                this.selectedOnly = selectedOnly;
                putValue(Action.NAME, selectedOnly ? Localization.menuTitle("Export selected entries") : Localization
                        .menuTitle("Export"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                Globals.exportFactory = Globals.prefs.getExporterFactory(Globals.journalAbbreviationLoader);
                FileDialogConfiguration fileDialogConfiguration = ExportAction.createExportFileChooser(Globals.exportFactory, Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY));
                DialogService dialogService = new FXDialogService();
                DefaultTaskExecutor.runInJavaFXThread(() ->
                        dialogService.showFileSaveDialog(fileDialogConfiguration)
                                .ifPresent(path -> export(path, fileDialogConfiguration.getSelectedExtensionFilter(), Globals.exportFactory.getExporters())));
            }

            private void export(Path file, FileChooser.ExtensionFilter selectedExtensionFilter, List<Exporter> exporters) {
                String selectedExtension = selectedExtensionFilter.getExtensions().get(0).replace("*", "");
                if (!file.endsWith(selectedExtension)) {
                    FileUtil.addExtension(file, selectedExtension);
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
                Globals.prefs.fileDirForDatabase = frame.getCurrentBasePanel().getBibDatabaseContext()
                        .getFileDirectories(Globals.prefs.getFileDirectoryPreferences());

                // Make sure we remember which filter was used, to set
                // the default for next time:
                Globals.prefs.put(JabRefPreferences.LAST_USED_EXPORT, format.getDescription());
                Globals.prefs.put(JabRefPreferences.EXPORT_WORKING_DIRECTORY, file.getParent().toString());

                final List<BibEntry> finEntries = entries;
                AbstractWorker exportWorker = new AbstractWorker() {

                    String errorMessage;

                    @Override
                    public void run() {
                        try {
                            format.export(frame.getCurrentBasePanel().getBibDatabaseContext(),
                                    file,
                                    frame.getCurrentBasePanel().getBibDatabaseContext().getMetaData().getEncoding()
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
                            JOptionPane.showMessageDialog(frame,
                                    Localization.lang("Could not save file.") + "\n" + errorMessage,
                                    Localization.lang("Save library"), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };

                // Run the export action in a background thread:
                exportWorker.getWorker().run();
                // Run the update method:
                exportWorker.update();
            }
        }

        return new InternalExportAction(frame, selectedOnly);
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
