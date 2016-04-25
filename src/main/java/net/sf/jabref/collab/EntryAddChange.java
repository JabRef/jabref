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

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertEntry;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;

class EntryAddChange extends Change {

    private final BibEntry diskEntry;
    private final JScrollPane sp;


    public EntryAddChange(BibEntry diskEntry) {
        super(Localization.lang("Added entry"));
        this.diskEntry = diskEntry;

        PreviewPanel pp = new PreviewPanel(null, diskEntry, null, Globals.prefs.get(JabRefPreferences.PREVIEW_0));
        sp = new JScrollPane(pp);
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        diskEntry.setId(IdGenerator.next());
        panel.getDatabase().insertEntry(diskEntry);
        secondary.insertEntry(diskEntry);
        undoEdit.addEdit(new UndoableInsertEntry(panel.getDatabase(), diskEntry, panel));
        return true;
    }

    @Override
    public JComponent description() {
        return sp;
    }
}
