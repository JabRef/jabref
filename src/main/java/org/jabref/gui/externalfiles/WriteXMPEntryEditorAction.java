package org.jabref.gui.externalfiles;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.xml.transform.TransformerException;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XMPUtil;
import org.jabref.model.entry.BibEntry;

/**
 * Write XMP action for EntryEditor toolbar.
 */
public class WriteXMPEntryEditorAction extends AbstractAction {

    private final BasePanel panel;
    private final EntryEditor editor;
    private String message;


    public WriteXMPEntryEditorAction(BasePanel panel, EntryEditor editor) {
        this.panel = panel;
        this.editor = editor;
        // normally, the next call should be without "Localization.lang". However, the string "Write XMP" is also used in non-menu places and therefore, the translation must be also available at Localization.lang
        putValue(Action.NAME, Localization.lang("Write XMP"));
        putValue(Action.SMALL_ICON, IconTheme.JabRefIcon.WRITE_XMP.getIcon());
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Write BibTeXEntry as XMP-metadata to PDF."));
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        setEnabled(false);
        panel.output(Localization.lang("Writing XMP-metadata..."));
        panel.frame().setProgressBarIndeterminate(true);
        panel.frame().setProgressBarVisible(true);
        BibEntry entry = editor.getEntry();

        // Make a list of all PDFs linked from this entry:
        List<Path> files = entry.getFiles().stream()
                .filter(file -> file.getFileType().equalsIgnoreCase("pdf"))
                .map(file -> file.findIn(panel.getBibDatabaseContext(), Globals.prefs.getFileDirectoryPreferences()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        // We want to offload the actual work to a background thread, so we have a worker
        // thread:
        AbstractWorker worker = new WriteXMPWorker(files, entry);
        // Using Spin, we get a thread that gets synchronously offloaded to a new thread,
        // blocking the execution of this method:
        worker.getWorker().run();
        // After the worker thread finishes, we are unblocked and ready to print the
        // status message:
        panel.output(message);
        panel.frame().setProgressBarVisible(false);
        setEnabled(true);
    }

    class WriteXMPWorker extends AbstractWorker {

        private final List<Path> files;
        private final BibEntry entry;


        public WriteXMPWorker(List<Path> files, BibEntry entry) {

            this.files = files;
            this.entry = entry;
        }

        @Override
        public void run() {
            if (files.isEmpty()) {
                message = Localization.lang("No PDF linked") + ".\n";
            } else {
                int written = 0;
                int error = 0;
                for (Path file : files) {
                    if (!Files.exists(file)) {
                        if (files.size() == 1) {
                            message = Localization.lang("PDF does not exist");
                        }
                        error++;

                    } else {
                        try {
                            XMPUtil.writeXMP(file.toFile(), entry, panel.getDatabase(), Globals.prefs.getXMPPreferences());
                            if (files.size() == 1) {
                                message = Localization.lang("Wrote XMP-metadata");
                            }
                            written++;
                        } catch (IOException | TransformerException e) {
                            if (files.size() == 1) {
                                message = Localization.lang("Error while writing") + " '" + file.toString() + "'";
                            }
                            error++;

                        }
                    }
                }
                if (files.size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(Localization.lang("Finished writing XMP-metadata. Wrote to %0 file(s).",
                            String.valueOf(written)));
                    if (error > 0) {
                        sb.append(' ').append(Localization.lang("Error writing to %0 file(s).", String.valueOf(error)));
                    }
                    message = sb.toString();
                }
            }
        }
    }
}
