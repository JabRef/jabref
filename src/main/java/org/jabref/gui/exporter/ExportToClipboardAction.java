package org.jabref.gui.exporter;

import java.awt.datatransfer.ClipboardOwner;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.input.ClipboardContent;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.BaseAction;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportToClipboardAction implements BaseAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportToClipboardAction.class);

    private final BasePanel panel;

    public ExportToClipboardAction(BasePanel panel) {
        this.panel = panel;
    }

    @Override
    public void action() {
        if (panel == null) {
            return;
        }
        if (panel.getSelectedEntries().isEmpty()) {
            panel.output(Localization.lang("This operation requires one or more entries to be selected."));
            return;
        }

        List<Exporter> exporters = Globals.exportFactory.getExporters().stream()
                                                        .sorted(Comparator.comparing(Exporter::getName))
                                                        .collect(Collectors.toList());

        DefaultTaskExecutor.runInJavaFXThread(() -> {
            Optional<Exporter> selectedExporter = panel.frame().getDialogService().showChoiceDialogAndWait(Localization.lang("Export"), Localization.lang("Select export format"),
                    Localization.lang("Export"), exporters);

            selectedExporter.ifPresent(exporter -> BackgroundTask.wrap(() -> exportToClipboard(exporter))
                                                                 .onSuccess(panel::output)
                                                                 .executeWith(Globals.TASK_EXECUTOR));
        });

    }

    private String exportToClipboard(Exporter exporter) {
        // Set the global variable for this database's file directory before exporting,
        // so formatters can resolve linked files correctly.
        // (This is an ugly hack!)
        Globals.prefs.fileDirForDatabase = panel.getBibDatabaseContext()
                                                .getFileDirectories(Globals.prefs.getFileDirectoryPreferences());

        Path tmp = null;
        try {
            // To simplify the exporter API we simply do a normal export to a temporary
            // file, and read the contents afterwards:
            tmp = Files.createTempFile("jabrefCb", ".tmp");

            List<BibEntry> entries = panel.getSelectedEntries();

            // Write to file:
            exporter.export(panel.getBibDatabaseContext(), tmp,
                    panel.getBibDatabaseContext()
                         .getMetaData()
                         .getEncoding()
                         .orElse(Globals.prefs.getDefaultEncoding()),
                    entries);
            // Read the file and put the contents on the clipboard:
            StringBuilder sb = new StringBuilder();
            try (Reader reader = new InputStreamReader(Files.newInputStream(tmp, StandardOpenOption.DELETE_ON_CLOSE),
                    panel.getBibDatabaseContext()
                         .getMetaData()
                         .getEncoding()
                         .orElse(Globals.prefs.getDefaultEncoding()))) {
                int s;
                while ((s = reader.read()) != -1) {
                    sb.append((char) s);
                }
            }
            ClipboardOwner owner = (clipboard, content) -> {
                // Do nothing
            };
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putRtf(sb.toString());
            Globals.clipboardManager.setContent(clipboardContent);
            return Localization.lang("Entries exported to clipboard") + ": " + entries.size();

        } catch (Exception e) {
            LOGGER.error("Error exporting to clipboard", e);
            return Localization.lang("Error exporting to clipboard");
        } finally {
            // Clean up:
            if ((tmp != null) && Files.exists(tmp)) {
                try {
                    Files.delete(tmp);
                } catch (IOException e) {
                    LOGGER.info("Cannot delete temporary clipboard file", e);
                }
            }
        }
    }
}
