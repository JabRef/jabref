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
package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertString;

class StringAddChange extends Change {

    private final BibtexString string;

    private final InfoPane tp = new InfoPane();
    private final JScrollPane sp = new JScrollPane(tp);


    public StringAddChange(BibtexString string) {
        name = Globals.lang("Added string") + ": '" + string.getName() + '\'';
        this.string = string;

        tp.setText("<HTML><H2>" + Globals.lang("Added string") + "</H2><H3>" + Globals.lang("Label") + ":</H3>" + string.getName() + "<H3>" + Globals.lang("Content") + ":</H3>" + string.getContent() + "</HTML>");

    }

    @Override
    public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {

        if (panel.database().hasStringLabel(string.getName())) {
            // The name to change to is already in the database, so we can't comply.
            Globals.logger("Cannot add string '" + string.getName() + "' because the name "
                    + "is already in use.");
        }

        try {
            panel.database().addString(string);
            undoEdit.addEdit(new UndoableInsertString(panel, panel.database(), string));
        } catch (KeyCollisionException ex) {
            Globals.logger("Error: could not add string '" + string.getName() + "': " + ex.getMessage());
        }
        try {
            secondary.addString(new BibtexString(IdGenerator.next(), string.getName(),
                    string.getContent()));
        } catch (KeyCollisionException ex) {
            Globals.logger("Error: could not add string '" + string.getName() + "' to tmp database: " + ex.getMessage());
        }
        return true;
    }

    @Override
    JComponent description() {
        return sp;
    }

}
