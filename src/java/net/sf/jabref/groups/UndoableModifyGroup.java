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

class UndoableModifyGroup extends AbstractUndoableEdit {

    private Vector groups;
    private int index;
    private String name, regexp, field, oldName, oldRegexp, oldField;
    private GroupSelector gs;

    public UndoableModifyGroup
	(GroupSelector gs, Vector groups, int index,
	 String field, String name, String regexp,
	 String oldField, String oldName, String oldRegexp) {

	this.gs = gs;
	this.groups = groups;
	this.index = index;
	this.name = name;
	this.regexp = regexp;
	this.field = field;
	this.oldName = oldName;
	this.oldRegexp = oldRegexp;
	this.oldField = oldField;
    }

    public String getUndoPresentationName() {
	return "Undo: modify group";
    }

    public String getRedoPresentationName() {
	return "Redo: modify group";
    }

    public void undo() {
	super.undo();
	remove();
	insert(oldRegexp, oldName, oldField);
    }

    public void redo() {
	super.redo();
	remove();
	insert(regexp, name, field);
    }

    private void remove() {
	for (int i=0; i<GroupSelector.DIM; i++)
	    groups.removeElementAt(index);
    }

    private void insert(String one, String two, String three) {
	index = GroupSelector.findPos(groups, two);
	groups.add(index, one);
	groups.add(index, two);
	groups.add(index, three);
	gs.revalidateList();
    }

}
