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
import net.sf.jabref.gui.undo.UndoableInsertString;
import net.sf.jabref.gui.undo.UndoableStringChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.IdGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class StringChange extends Change {

    private final BibtexString string;
    private final String mem;
    private final String disk;
    private final String label;

    private final InfoPane tp = new InfoPane();
    private final JScrollPane sp = new JScrollPane(tp);
    private final BibtexString tmpString;

    private static final Log LOGGER = LogFactory.getLog(StringChange.class);


    public StringChange(BibtexString string, BibtexString tmpString, String label, String mem, String disk) {
        super(Localization.lang("Modified string") + ": '" + label + '\'');
        this.tmpString = tmpString;
        this.string = string;
        this.label = label;
        this.mem = mem;
        this.disk = disk;

        StringBuilder sb = new StringBuilder(46);
        sb.append("<HTML><H2>").append(Localization.lang("Modified string")).append("</H2><H3>")
                .append(Localization.lang("Label")).append(":</H3>").append(label).append("<H3>")
                .append(Localization.lang("New content")).append(":</H3>").append(disk);
        if (string == null) {
            sb.append("<P><I>");
            sb.append(Localization.lang("Cannot merge this change")).append(": ");
            sb.append(Localization.lang("The string has been removed locally")).append("</I>");
        } else {
            sb.append("<H3>");
            sb.append(Localization.lang("Current content")).append(":</H3>");
            sb.append(string.getContent());
        }
        sb.append("</HTML>");
        tp.setText(sb.toString());
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        if (string == null) {
            // The string was removed or renamed locally. We guess that it was removed.
            String newId = IdGenerator.next();
            BibtexString bs = new BibtexString(newId, label, disk);
            try {
                panel.getDatabase().addString(bs);
                undoEdit.addEdit(new UndoableInsertString(panel, panel.getDatabase(), bs));
            } catch (KeyCollisionException ex) {
                LOGGER.info("Error: could not add string '" + bs.getName() + "': " + ex.getMessage(), ex);
            }
        } else {
            string.setContent(disk);
            undoEdit.addEdit(new UndoableStringChange(panel, string, false, mem, disk));
        }

        // Update tmp database:
        if (tmpString == null) {
            BibtexString bs = new BibtexString(IdGenerator.next(), label, disk);
            secondary.addString(bs);
        } else {
            tmpString.setContent(disk);
        }

        return true;
    }

    @Override
    public JComponent description() {
        return sp;
    }

}
