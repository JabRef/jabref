/*  Copyright (C) 2003-2011 JabRef contributors.
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

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;

import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * Try to download fulltext PDF for selected entry(ies) by following URL or DOI link.
 */
public class FindFullTextAction extends AbstractWorker {

    private final BasePanel basePanel;
    private BibtexEntry entry;
    private Optional<URL> result;


    public FindFullTextAction(BasePanel basePanel) {
        this.basePanel = basePanel;
    }

    @Override
    public void init() throws Throwable {
        basePanel.output(Localization.lang("Looking for full text document..."));
    }

    @Override
    public void run() {
        // TODO: just download for all entries and save files without dialog
        entry = basePanel.getSelectedEntries()[0];
        FindFullText fft = new FindFullText();
        result = fft.findFullText(entry);
    }

    @Override
    public void update() {
        if (result.isPresent()) {
            String bibtexKey = entry.getCiteKey();
            String[] dirs = basePanel.metaData().getFileDirectory(GUIGlobals.FILE_FIELD);
            if (dirs.length == 0) {
                // FIXME: Localization
                JOptionPane.showMessageDialog(basePanel.frame(), "Main file directory not set! Preferences -> External programs", "Directory not found", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // TODO: this needs its own thread as it blocks the UI!
            DownloadExternalFile def = new DownloadExternalFile(basePanel.frame(), basePanel.metaData(), bibtexKey);
            try {
                def.download(result.get(), new DownloadExternalFile.DownloadCallback() {
                    @Override
                    public void downloadComplete(FileListEntry file) {
                        FileListTableModel tm = new FileListTableModel();
                        String oldValue = entry.getField(GUIGlobals.FILE_FIELD);
                        tm.setContent(oldValue);
                        tm.addEntry(tm.getRowCount(), file);
                        String newValue = tm.getStringRepresentation();
                        UndoableFieldChange edit = new UndoableFieldChange(entry, GUIGlobals.FILE_FIELD, oldValue, newValue);
                        entry.setField(GUIGlobals.FILE_FIELD, newValue);
                        basePanel.undoManager.addEdit(edit);
                        basePanel.markBaseChanged();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            basePanel.output(Localization.lang("Finished downloading full text document"));
        }
        else {
            String message = Localization.lang("Full text article download failed");
            basePanel.output(message);
            JOptionPane.showMessageDialog(basePanel.frame(), message, message, JOptionPane.ERROR_MESSAGE);
        }
    }
}
