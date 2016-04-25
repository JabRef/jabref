/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.external;

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
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.xmp.XMPUtil;
import net.sf.jabref.model.entry.BibEntry;

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
        String pdf = entry.getField("pdf");
        List<String> dirs = panel.getBibDatabaseContext().getFileDirectory("pdf");
        FileUtil.expandFilename(pdf, dirs).ifPresent(files::add);

        // Then check the "file" field:
        dirs = panel.getBibDatabaseContext().getFileDirectory();
        if (entry.hasField(Globals.FILE_FIELD)) {
            FileListTableModel tm = new FileListTableModel();
            tm.setContent(entry.getField(Globals.FILE_FIELD));
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
                            XMPUtil.writeXMP(file, entry, panel.getDatabase());
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
