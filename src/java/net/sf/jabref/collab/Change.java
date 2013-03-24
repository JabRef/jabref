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
package net.sf.jabref.collab;

import net.sf.jabref.Globals;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.JComponent;
import net.sf.jabref.undo.NamedCompound;

public abstract class Change extends DefaultMutableTreeNode {

  String name;
  boolean accepted = true;

  public Change() {
    name = "";
  }

  public Change(String name) {
    this.name = Globals.lang(name);
  }

  public String getName() {
    return name;
  }

  public String toString() {
    return getName();
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
    if ((getParent() != null) && (getParent() instanceof Change))
      return ((Change)getParent()).isAccepted();
    else
      return true;
  }

  /**
   * This method returns a JComponent detailing the nature of the change.
   * @return JComponent
   */
  abstract JComponent description();

  /**
  * Perform the change. This method is responsible for adding a proper undo edit to
  * the NamedCompound, so the change can be undone.
  * @param panel BasePanel The tab where the database lives.
  * @param secondary BibtexDatabase The "tmp" database for which the change
  *   should also be made.
  * @param undoEdit NamedCompound The compound to hold the undo edits.
  * @return true if all changes were made, false if not all were accepted.
  */
  abstract boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit);

}
