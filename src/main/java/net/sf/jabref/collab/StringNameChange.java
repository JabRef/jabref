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
import javax.swing.JLabel;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibtexString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableInsertString;
import net.sf.jabref.gui.undo.UndoableStringChange;

class StringNameChange extends Change {

    private final BibtexString string;
    private final String mem;
    private final String disk;
    private final String content;
    private final BibtexString tmpString;

    private static final Log LOGGER = LogFactory.getLog(StringNameChange.class);


    public StringNameChange(BibtexString string, BibtexString tmpString,
            String mem, String tmp, String disk, String content) {
        super(Localization.lang("Renamed string") + ": '" + tmp + '\'');
        this.tmpString = tmpString;
        this.string = string;
        this.content = content;
        this.mem = mem;
        this.disk = disk;

    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {

        if (panel.getDatabase().hasStringLabel(disk)) {
            // The name to change to is already in the database, so we can't comply.
            LOGGER.info("Cannot rename string '" + mem + "' to '" + disk + "' because the name "
                    + "is already in use.");
        }

        if (string == null) {
            // The string was removed or renamed locally. We guess that it was removed.
            String newId = IdGenerator.next();
            BibtexString bs = new BibtexString(newId, disk, content);
            try {
                panel.getDatabase().addString(bs);
                undoEdit.addEdit(new UndoableInsertString(panel, panel.getDatabase(), bs));
            } catch (KeyCollisionException ex) {
                LOGGER.info("Error: could not add string '" + bs.getName() + "': " + ex.getMessage(), ex);
            }
        } else {
            string.setName(disk);
            undoEdit.addEdit(new UndoableStringChange(panel, string, true, mem, disk));
        }

        // Update tmp database:
        if (tmpString == null) {
            String newId = IdGenerator.next();
            BibtexString bs = new BibtexString(newId, disk, content);
            secondary.addString(bs);
        } else {
            tmpString.setName(disk);
        }

        return true;
    }

    @Override
    public JComponent description() {
        return new JLabel(disk + " : " + content);
    }

}
