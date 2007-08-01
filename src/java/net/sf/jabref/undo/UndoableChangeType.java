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

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;

/**
 * This class represents the change of type for an entry.
 */
public class UndoableChangeType extends AbstractUndoableEdit {

    BibtexEntryType oldType, newType;
    BibtexEntry be;

    public UndoableChangeType(BibtexEntry be, BibtexEntryType oldType,
			      BibtexEntryType newType) {
	this.oldType = oldType;
	this.newType = newType;
	this.be = be;
    }

    public String getUndoPresentationName() {
	return "Undo: change type";
    }

    public String getRedoPresentationName() {
	return "Redo: change type";
    }

    public void undo() {
	super.undo();
	be.setType(oldType);
    }

    public void redo() {
	super.redo();
	be.setType(newType);
    }

}
