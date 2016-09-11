package net.sf.jabref.gui.externalfiles;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.xml.transform.TransformerException;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.filelist.FileListEntry;
import net.sf.jabref.gui.filelist.FileListTableModel;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.xmp.XMPUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

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
        List<File> files = new ArrayList<>();

        // First check the (legacy) "pdf" field:
        entry.getField(FieldName.PDF)
                .ifPresent(pdf -> FileUtil.expandFilename(pdf, panel.getBibDatabaseContext()
                        .getFileDirectory(FieldName.PDF, Globals.prefs.getFileDirectoryPreferences()))
                .ifPresent(files::add));

        // Then check the "file" field:
        List<String> dirs = panel.getBibDatabaseContext().getFileDirectory(Globals.prefs.getFileDirectoryPreferences());
        if (entry.hasField(FieldName.FILE)) {
            FileListTableModel tm = new FileListTableModel();
            entry.getField(FieldName.FILE).ifPresent(tm::setContent);
            for (int j = 0; j < tm.getRowCount(); j++) {
                FileListEntry flEntry = tm.getEntry(j);
                if ((flEntry.type.isPresent()) && "pdf".equalsIgnoreCase(flEntry.type.get().getName())) {
                    FileUtil.expandFilename(flEntry.link, dirs).ifPresent(files::add);
                }
            }
        }

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

        private final List<File> files;
        private final BibEntry entry;


        public WriteXMPWorker(List<File> files, BibEntry entry) {

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
                for (File file : files) {
                    if (!file.exists()) {
                        if (files.size() == 1) {
                            message = Localization.lang("PDF does not exist");
                        }
                        error++;

                    } else {
                        try {
                            XMPUtil.writeXMP(file, entry, panel.getDatabase(), Globals.prefs.getXMPPreferences());
                            if (files.size() == 1) {
                                message = Localization.lang("Wrote XMP-metadata");
                            }
                            written++;
                        } catch (IOException | TransformerException e) {
                            if (files.size() == 1) {
                                message = Localization.lang("Error while writing") + " '" + file.getPath() + "'";
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
