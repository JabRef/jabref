/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

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
package net.sf.jabref.groups;

import javax.swing.undo.*;
import java.util.Vector;

class UndoableAddOrRemoveGroup extends AbstractUndoableEdit {

    private Vector groups;
    private int index;
    private boolean addition;
    private String name, regexp, field;
    private GroupSelector gs;

    public UndoableAddOrRemoveGroup
	(GroupSelector gs, Vector groups, int index, boolean addition,
	 String field, String name, String regexp) {

	this.gs = gs;
	this.addition = addition;
	this.groups = groups;
	this.index = index;
	this.name = name;
	this.regexp = regexp;
	this.field = field;
    }

    public String getUndoPresentationName() {
	return "Undo: "+(addition ? "add group" : "remove group");
    }

    public String getRedoPresentationName() {
	return "Redo: "+(addition ? "add group" : "remove group");
    }

    public void undo() {
	super.undo();
	doOperation(!addition);
    }

    public void redo() {
	super.redo();
	doOperation(addition);
    }

    private void doOperation(boolean add) {
	if (add) {
	    groups.add(index, regexp);
	    groups.add(index, name);
	    groups.add(index, field);
	} else { // Remove
	    for (int i=0; i<GroupSelector.DIM; i++)
		groups.removeElementAt(index);
	}
	gs.revalidateList();
    }

}
