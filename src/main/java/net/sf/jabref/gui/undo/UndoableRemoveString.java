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
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibtexString;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UndoableRemoveString extends AbstractUndoableEdit {

    private final BibDatabase base;
    private final BibtexString string;
    private final BasePanel panel;

    private static final Log LOGGER = LogFactory.getLog(UndoableRemoveString.class);

    public UndoableRemoveString(BasePanel panel,
            BibDatabase base, BibtexString string) {
        this.base = base;
        this.string = string;
        this.panel = panel;
    }

    @Override
    public String getUndoPresentationName() {
        return Localization.lang("Undo") + ": " +
                Localization.lang("remove string");
    }

    @Override
    public String getRedoPresentationName() {
        return Localization.lang("Redo") + ": " +
                Localization.lang("remove string");
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        try {
            base.addString(string);
        } catch (KeyCollisionException ex) {
            LOGGER.warn("Problem to undo `remove string`", ex);
        }

        panel.updateStringDialog();
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        base.removeString(string.getId());

        panel.updateStringDialog();
    }

}
