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
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.IdGenerator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class StringAddChange extends Change {

    private final BibtexString string;

    private final InfoPane tp = new InfoPane();
    private final JScrollPane sp = new JScrollPane(tp);

    private static final Log LOGGER = LogFactory.getLog(StringAddChange.class);


    public StringAddChange(BibtexString string) {
        super(Localization.lang("Added string") + ": '" + string.getName() + '\'');
        this.string = string;
        tp.setText("<HTML><H2>" + Localization.lang("Added string") + "</H2><H3>" +
                Localization.lang("Label") + ":</H3>" + string.getName() + "<H3>" +
                Localization.lang("Content") + ":</H3>" + string.getContent() + "</HTML>");
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {

        if (panel.getDatabase().hasStringLabel(string.getName())) {
            // The name to change to is already in the database, so we can't comply.
            LOGGER.info("Cannot add string '" + string.getName() + "' because the name "
                    + "is already in use.");
        }

        try {
            panel.getDatabase().addString(string);
            undoEdit.addEdit(new UndoableInsertString(panel, panel.getDatabase(), string));
        } catch (KeyCollisionException ex) {
            LOGGER.info("Error: could not add string '" + string.getName() + "': " + ex.getMessage(), ex);
        }
        try {
            secondary.addString(new BibtexString(IdGenerator.next(), string.getName(),
                    string.getContent()));
        } catch (KeyCollisionException ex) {
            LOGGER.info("Error: could not add string '" + string.getName() + "' to tmp database: " + ex.getMessage(), ex);
        }
        return true;
    }

    @Override
    public JComponent description() {
        return sp;
    }

}
