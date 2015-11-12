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
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

/**
 * This class represents the removal of an entry. The constructor needs
 * references to the database, the entry, and the map of open entry editors.
 * The latter to be able to close the entry's editor if it is opened before
 * the insert is undone.
 */
public class UndoableInsertEntry extends AbstractUndoableEdit {

    private final BibtexDatabase base;
    private final BibtexEntry entry;
    private final BasePanel panel;


    public UndoableInsertEntry(BibtexDatabase base, BibtexEntry entry,
                               BasePanel panel) {
        this.base = base;
        this.entry = entry;
        this.panel = panel;
    }

    @Override
    public String getUndoPresentationName() {
        return "Undo: insert entry";
    }

    @Override
    public String getRedoPresentationName() {
        return "Redo: insert entry";
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        try {
            base.removeEntry(entry.getId());
            // If the entry has an editor currently open, we must close it.
            panel.ensureNotShowing(entry);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change
        String id = IdGenerator.next();
        entry.setId(id);
        base.insertEntry(entry);
    }

}
