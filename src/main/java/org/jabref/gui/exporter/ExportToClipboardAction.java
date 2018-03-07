package org.jabref.gui.exporter;

import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportToClipboardAction extends AbstractWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExportToClipboardAction.class);

    private final JabRefFrame frame;

    /**
     * written by run() and read by update()
     */
    private String message;

    public ExportToClipboardAction(JabRefFrame frame) {
        this.frame = Objects.requireNonNull(frame);
    }

    @Override
    public void run() {
        BasePanel panel = frame.getCurrentBasePanel();
        if (panel == null) {
            return;
        }
        if (panel.getSelectedEntries().isEmpty()) {
            message = Localization.lang("This operation requires one or more entries to be selected.");
            getCallBack().update();
            return;
        }

        List<Exporter> exporters = new ArrayList<>(Globals.exportFactory.getExporters());

        Optional<Exporter> selectedExporter = frame.getDialogService().showChoiceDialogAndWait(Localization.lang("Export"), Localization.lang("Select export format"),
                Localization.lang("Export"), null, exporters);

        if (!selectedExporter.isPresent()) {
            return;
        }

        Exporter exporter = selectedExporter.get();

        // Set the global variable for this database's file directory before exporting,
        // so formatters can resolve linked files correctly.
        // (This is an ugly hack!)
        Globals.prefs.fileDirForDatabase = frame.getCurrentBasePanel()
                .getBibDatabaseContext()
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
            RtfTransferable rs = new RtfTransferable(sb.toString());
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(rs, owner);
            message = Localization.lang("Entries exported to clipboard") + ": " + entries.size();

        } catch (Exception e) {
            LOGGER.error("Error exporting to clipboard", e); //To change body of catch statement use File | Settings | File Templates.
            message = Localization.lang("Error exporting to clipboard");
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

    @Override
    public void update() {
        frame.output(message);
    }

}
