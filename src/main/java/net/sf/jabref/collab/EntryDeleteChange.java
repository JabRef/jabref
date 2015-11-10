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
package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableRemoveEntry;
import net.sf.jabref.bibtex.DuplicateCheck;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

class EntryDeleteChange extends Change {

    private final BibtexEntry memEntry;
    private final BibtexEntry tmpEntry;
    BibtexEntry diskEntry;
    private final JScrollPane sp;

    private static final Log LOGGER = LogFactory.getLog(EntryDeleteChange.class);


    public EntryDeleteChange(BibtexEntry memEntry, BibtexEntry tmpEntry) {
        super("Deleted entry");
        this.memEntry = memEntry;
        this.tmpEntry = tmpEntry;

        // Compare the deleted entry in memory with the one in the tmpfile. The
        // entry could have been removed in memory.
        double matchWithTmp = DuplicateCheck.compareEntriesStrictly(memEntry, tmpEntry);

        // Check if it has been modified locally, since last tempfile was saved.
        boolean isModifiedLocally = !(matchWithTmp > 1);

        LOGGER.debug("Modified entry: " + memEntry.getCiteKey() + "\n Modified locally: " + isModifiedLocally);

        PreviewPanel pp = new PreviewPanel(null, memEntry, null, new MetaData(), Globals.prefs.get(JabRefPreferences.PREVIEW_0));
        sp = new JScrollPane(pp);
    }

    @Override
    public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {
        panel.database().removeEntry(memEntry.getId());
        undoEdit.addEdit(new UndoableRemoveEntry(panel.database(), memEntry, panel));
        secondary.removeEntry(tmpEntry.getId());
        return true;
    }

    @Override
    JComponent description() {
        return sp;
    }
}
