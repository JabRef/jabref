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
import net.sf.jabref.Globals;

/**
 * This class represents a change in any field value. The relevant
 * information is the BibtexEntry, the field name, the old and the
 * new value. Old/new values can be null.
 */
public class UndoablePreambleChange extends AbstractUndoableEdit {

    private BibtexDatabase base;
    private String oldValue, newValue;
    private BasePanel panel;

    public UndoablePreambleChange(BibtexDatabase base, BasePanel panel,
				  String oldValue, String newValue) {
	this.base = base;
	this.oldValue = oldValue;
	this.newValue = newValue;
	this.panel = panel;
    }

    public String getUndoPresentationName() {
	return Globals.lang("Undo")+": "+Globals.lang("change preamble");
    }

    public String getRedoPresentationName() {
	return Globals.lang("Redo")+": "+Globals.lang("change preamble");
    }

    public void undo() {
	super.undo();
	
	// Revert the change.
	base.setPreamble(oldValue);

	// If the preamble editor is open, update it.
	panel.updatePreamble();
    }

    public void redo() {
	super.redo();

	// Redo the change.
	base.setPreamble(newValue);

	// If the preamble editor is open, update it.
	panel.updatePreamble();

    }



}
