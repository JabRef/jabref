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

import javax.swing.undo.*;
import net.sf.jabref.*;

public class UndoableMoveString extends AbstractUndoableEdit {

    private BasePanel panel;
    private BibtexDatabase base;
    private boolean up;
    private int pos;

    public UndoableMoveString(BasePanel panel,
			      BibtexDatabase base,
			      int pos, boolean up) {
	this.panel = panel;
	this.base = base;
	this.pos = pos;
	this.up = up;
    }

    public String getUndoPresentationName() {
	return "Undo: move string "+(up ? "up" : "down");
    }

    public String getRedoPresentationName() {
	return "Redo: move string "+(up ? "up" : "down");
    }

    public void undo() {
	super.undo();
	
	// Revert the change.
	panel.assureStringDialogNotEditing();
	moveString(!up, (up ? pos-1 : pos+1));
	panel.updateStringDialog();
    }

    public void redo() {
	super.redo();

	// Redo the change.
	panel.assureStringDialogNotEditing();
	moveString(up, pos);
	panel.updateStringDialog();
    }

    private void moveString(boolean up, int pos) {
	BibtexString bs = base.getString(pos);
	base.removeString(pos);
	try {
	    base.addString(bs, (up ? pos-1 : pos+1));
	} catch (KeyCollisionException ex) {}
    }



}
