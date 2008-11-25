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

import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.swing.event.UndoableEditEvent;

import net.sf.jabref.BasePanel;

public class CountingUndoManager extends UndoManager {

    private int unchangedPoint = 0,
	current = 0;
    private BasePanel panel = null;

    public CountingUndoManager(BasePanel basePanel) {
    	super();
	    panel = basePanel;
    }


    public synchronized boolean addEdit(UndoableEdit edit) {
	    current++;
        return super.addEdit(edit);
    }
    
    public synchronized void undo() throws CannotUndoException {
	    super.undo();
	    current--;
        panel.updateEntryEditorIfShowing();
        //panel.updateViewToSelected();
        /*SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                panel.updateViewToSelected();
            }
        });*/

    }

    public synchronized void redo() throws CannotUndoException {
	    super.redo();
	    current++;
        panel.updateEntryEditorIfShowing();
        //panel.updateViewToSelected();
        /*SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                panel.updateViewToSelected();
            }
        });*/
    }

    public synchronized void markUnchanged() {
    	unchangedPoint = current;
    }

    public boolean hasChanged() {
    	return !(current == unchangedPoint);
    }
}
