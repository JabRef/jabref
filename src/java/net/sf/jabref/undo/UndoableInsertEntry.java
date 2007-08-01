/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref.undo;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;

/**
 * This class represents the removal of an entry. The constructor needs
 * references to the database, the entry, and the map of open entry editors.
 * The latter to be able to close the entry's editor if it is opened before
 * the insert is undone.
 */
public class UndoableInsertEntry extends AbstractUndoableEdit {

    private BibtexDatabase base;
    private BibtexEntry entry;
    private BasePanel panel;

    public UndoableInsertEntry(BibtexDatabase base, BibtexEntry entry,
			       BasePanel panel) {
	this.base = base;
	this.entry = entry;
	this.panel = panel;
    }

    public String getUndoPresentationName() {
	return "Undo: insert entry";
    }

    public String getRedoPresentationName() {
	return "Redo: insert entry";
    }

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

    public void redo() {
	super.redo();

	// Redo the change.
	try {
          String id = Util.createNeutralId();
	    entry.setId(id);
	    base.insertEntry(entry);
	} catch (Throwable ex) {
          ex.printStackTrace();
	}
    }



}
