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
package net.sf.jabref.undo;

import javax.swing.undo.AbstractUndoableEdit;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Util;

/**
 * This class represents a change in any field value. The relevant
 * information is the BibtexEntry, the field name, the old and the
 * new value. Old/new values can be null.
 */
public class UndoableFieldChange extends AbstractUndoableEdit {

    private BibtexEntry entry;
    private String field;
    private String oldValue, newValue;

    public UndoableFieldChange(BibtexEntry entry, String field,
			       String oldValue, String newValue) {
	this.entry = entry;
	this.field = field;
	this.oldValue = oldValue;
	this.newValue = newValue;
    }

    public String getUndoPresentationName() {
	return "Undo: change field";
    }

    public String getRedoPresentationName() {
	return "Redo: change field";
    }

    public void undo() {
	super.undo();

	// Revert the change.
	try {
          if (oldValue != null)
            entry.setField(field, oldValue);
          else
            entry.clearField(field);

	} catch (Throwable ex) {
	    Util.pr(ex.getMessage());
	}
    }

    public void redo() {
	super.redo();

	// Redo the change.
	try {
          if (newValue != null)
            entry.setField(field, newValue);
          else
            entry.clearField(field);

	} catch (Throwable ex) {
	    Util.pr(ex.getMessage());
	}
    }



}
