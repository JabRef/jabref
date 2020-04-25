package org.jabref.gui.exporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.scene.input.ClipboardContent;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportToClipboardAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportToClipboardAction.class);

    // Only text based exporters can be used
    private static final List<String> SUPPORTED_FILETYPES = Arrays.asList("txt", "rtf", "rdf", "xml", "html", "htm", "csv", "ris");

    private JabRefFrame frame;
    private final DialogService dialogService;
    private BasePanel panel;
    private final List<BibEntry> entries = new ArrayList<>();

    public ExportToClipboardAction(JabRefFrame frame, DialogService dialogService) {
        this.frame = frame;
        this.dialogService = dialogService;
    }

    public ExportToClipboardAction(BasePanel panel, DialogService dialogService) {
        this.panel = panel;
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        if (panel == null) {
            panel = frame.getCurrentBasePanel();
        }

        if (panel.getSelectedEntries().isEmpty()) {
            dialogService.notify(Localization.lang("This operation requires one or more entries to be selected."));
            return;
        }

        List<Exporter> exporters = Globals.exportFactory.getExporters().stream()
                                                        .sorted(Comparator.comparing(Exporter::getName))
                                                        .filter(exporter -> SUPPORTED_FILETYPES.containsAll(exporter.getFileType().getExtensions()))
                                                        .collect(Collectors.toList());

        // Find default choice, if any
        Exporter defaultChoice = exporters.stream()
                                          .filter(exporter -> exporter.getName().equals(Globals.prefs.get(JabRefPreferences.LAST_USED_EXPORT)))
                                          .findAny()
                                          .orElse(null);

        Optional<Exporter> selectedExporter = dialogService.showChoiceDialogAndWait(Localization.lang("Export"), Localization.lang("Select export format"),
                Localization.lang("Export"), defaultChoice, exporters);

        selectedExporter.ifPresent(exporter -> BackgroundTask.wrap(() -> exportToClipboard(exporter))
                                                                .onSuccess(this::setContentToClipboard)
                                                                .onFailure(ex -> { /* swallow as already logged */ })
                                                                .executeWith(Globals.TASK_EXECUTOR));
    }

    private ExportResult exportToClipboard(Exporter exporter) throws Exception {
        // Set the global variable for this database's file directory before exporting,
        // so formatters can resolve linked files correctly.
        // (This is an ugly hack!)
        Globals.prefs.fileDirForDatabase = panel.getBibDatabaseContext().getFileDirectoriesAsPaths(Globals.prefs.getFilePreferences()).stream().map(Path::toString).collect(Collectors.toList());

        // Add chosen export type to last used pref, to become default
        Globals.prefs.put(JabRefPreferences.LAST_USED_EXPORT, exporter.getName());

        Path tmp = null;
        try {
            // To simplify the exporter API we simply do a normal export to a temporary
            // file, and read the contents afterwards:
            tmp = Files.createTempFile("jabrefCb", ".tmp");

            entries.addAll(panel.getSelectedEntries());

            // Write to file:
            exporter.export(panel.getBibDatabaseContext(), tmp,
                            panel.getBibDatabaseContext()
                                 .getMetaData()
                                 .getEncoding()
                                 .orElse(Globals.prefs.getDefaultEncoding()),
                            entries);
            // Read the file and put the contents on the clipboard:

            return new ExportResult(readFileToString(tmp), exporter.getFileType());
        } catch (Exception e) {
            LOGGER.error("Error exporting to clipboard", e);
            throw new Exception("Rethrow ", e);
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

    private void setContentToClipboard(ExportResult result) {
        ClipboardContent clipboardContent = new ClipboardContent();
        List<String> extensions = result.fileType.getExtensions();
        if (extensions.contains("html")) {
            clipboardContent.putHtml(result.content);
        } else if (extensions.contains("rtf")) {
            clipboardContent.putRtf(result.content);
        } else if (extensions.contains("rdf")) {
            clipboardContent.putRtf(result.content);
        }
        clipboardContent.putString(result.content);
        Globals.clipboardManager.setContent(clipboardContent);

        dialogService.notify(Localization.lang("Entries exported to clipboard") + ": " + entries.size());

    }

    private String readFileToString(Path tmp) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(tmp, panel.getBibDatabaseContext()
                                                                       .getMetaData()
                                                                       .getEncoding()
                                                                       .orElse(Globals.prefs.getDefaultEncoding()))) {
            return reader.lines().collect(Collectors.joining(OS.NEWLINE));
        }
    }

    private static class ExportResult {
        final String content;
        final FileType fileType;

        ExportResult(String content, FileType fileType) {
            this.content = content;
            this.fileType = fileType;
        }
    }
}
