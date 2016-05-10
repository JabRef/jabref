/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.model.database.BibDatabase;

abstract class Change extends DefaultMutableTreeNode {

    protected String name;
    private boolean accepted = true;


    Change() {
        name = "";
    }

    Change(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean a) {
        accepted = a;
    }

    /**
     * This method is used to disable the "accept" box if the parent has been set to "not accepted".
     * Thus the user can disable e.g. an entry change without having to disable all field changes.
     * @return boolean false if the parent overrides by not being accepted.
     */
    public boolean isAcceptable() {
        if ((getParent() != null) && (getParent() instanceof Change)) {
            return ((Change) getParent()).isAccepted();
        } else {
            return true;
        }
    }

    /**
     * This method returns a JComponent detailing the nature of the change.
     * @return JComponent
     */
    public abstract JComponent description();

    /**
     * Perform the change. This method is responsible for adding a proper undo edit to
     * the NamedCompound, so the change can be undone.
     * @param panel BasePanel The tab where the database lives.
     * @param secondary BibDatabase The "tmp" database for which the change
     *   should also be made.
     * @param undoEdit NamedCompound The compound to hold the undo edits.
     * @return true if all changes were made, false if not all were accepted.
     */
    public abstract boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit);

}
