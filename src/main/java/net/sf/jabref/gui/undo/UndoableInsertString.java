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

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexString;

public class UndoableInsertString extends AbstractUndoableEdit {

    private final BibtexDatabase base;
    private final BasePanel panel;
    private final BibtexString string;


    public UndoableInsertString(BasePanel panel, BibtexDatabase base,
            BibtexString string) {
        this.base = base;
        this.panel = panel;
        this.string = string;
    }

    @Override
    public String getUndoPresentationName() {
        // @formatter:off
        return Localization.lang("Undo") + ": " +
                Localization.lang("insert string");
        // @formatter:on
    }

    @Override
    public String getRedoPresentationName() {
        // @formatter:off
        return Localization.lang("Redo") + ": " +
                Localization.lang("insert string");
        // @formatter:on
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        base.removeString(string.getId());
        panel.updateStringDialog();
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        try {
            base.addString(string);
        } catch (KeyCollisionException ex) {
            ex.printStackTrace();
        }

        panel.updateStringDialog();
    }

}
