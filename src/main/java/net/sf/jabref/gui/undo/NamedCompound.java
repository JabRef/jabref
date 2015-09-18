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
package net.sf.jabref.gui.undo;

import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import net.sf.jabref.logic.l10n.Localization;

public class NamedCompound extends CompoundEdit {

    private final String name;
    private boolean hasEdits;


    public NamedCompound(String name) {
        super();
        this.name = name;
    }

    @Override
    public boolean addEdit(UndoableEdit undoableEdit) {
        hasEdits = true;
        return super.addEdit(undoableEdit);
    }

    public boolean hasEdits() {
        return hasEdits;
    }

    @Override
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": " + name;
    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": " + name;
    }

    /**
     * Returns the name of this compound, without the Undo or Redo prefix.
     */
    public String getNameOnly() {
        return name;
    }
}
