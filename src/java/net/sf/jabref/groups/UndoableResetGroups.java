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

class UndoableResetGroups extends AbstractUndoableEdit {

    private Vector groups;
    private Vector originalGroups;
    private GroupSelector gs;
    private boolean revalidate = true;

    public UndoableResetGroups(GroupSelector gs, Vector groups) {
        this.groups = groups;
        this.originalGroups = (Vector) groups.clone();
        this.gs = gs;
    }

    public String getUndoPresentationName() {
        return "Undo: clear group";
    }

    public String getRedoPresentationName() {
        return "Redo: clear group";
    }

    public void undo() {
        super.undo();
        groups.addAll(originalGroups);
    	if (revalidate)
    	    gs.revalidateList();
    }

    public void redo() {
        super.redo();
	    groups.clear();
		if (revalidate)
		    gs.revalidateList();	    
    }


    /**
     * Call this method to decide if the group list should be immediately
     * revalidated by this operation. Default is true.
     *
     * @param val a <code>boolean</code> value
     */
    public void setRevalidate(boolean val) {
        revalidate = val;
    }
}
