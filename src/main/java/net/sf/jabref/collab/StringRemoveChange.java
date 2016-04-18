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

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableRemoveString;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibtexString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class StringRemoveChange extends Change {
    private final BibtexString string;
    private final BibtexString inMem;

    private final InfoPane tp = new InfoPane();
    private final JScrollPane sp = new JScrollPane(tp);
    private final BibtexString tmpString;

    private static final Log LOGGER = LogFactory.getLog(StringRemoveChange.class);


    public StringRemoveChange(BibtexString string, BibtexString tmpString, BibtexString inMem) {
        super(Localization.lang("Removed string") + ": '" + string.getName() + '\'');
        this.tmpString = tmpString;
        this.string = string;
        this.inMem = inMem; // Holds the version in memory. Check if it has been modified...?

        tp.setText("<HTML><H2>" + Localization.lang("Removed string") + "</H2><H3>" +
                Localization.lang("Label") + ":</H3>" + string.getName() + "<H3>" +
                Localization.lang("Content") + ":</H3>" + string.getContent() + "</HTML>");
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {

        try {
            panel.getDatabase().removeString(inMem.getId());
            undoEdit.addEdit(new UndoableRemoveString(panel, panel.getDatabase(), string));
        } catch (Exception ex) {
            LOGGER.info("Error: could not add string '" + string.getName() + "': " + ex.getMessage(), ex);
        }

        // Update tmp database:
        secondary.removeString(tmpString.getId());

        return true;
    }

    @Override
    public JComponent description() {
        return sp;
    }

}
