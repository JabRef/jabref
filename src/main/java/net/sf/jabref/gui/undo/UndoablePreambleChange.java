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
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.logic.l10n.Localization;

/**
 * This class represents a change in any field value. The relevant
 * information is the BibEntry, the field name, the old and the
 * new value. Old/new values can be null.
 */
public class UndoablePreambleChange extends AbstractUndoableEdit {

    private final BibDatabase base;
    private final String oldValue;
    private final String newValue;
    private final BasePanel panel;


    public UndoablePreambleChange(BibDatabase base, BasePanel panel,
            String oldValue, String newValue) {
        this.base = base;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.panel = panel;
    }

    @Override
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": " +
                Localization.lang("change preamble");
    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": " +
                Localization.lang("change preamble");
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        base.setPreamble(oldValue);

        // If the preamble editor is open, update it.
        panel.updatePreamble();
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        base.setPreamble(newValue);

        // If the preamble editor is open, update it.
        panel.updatePreamble();

    }

}
