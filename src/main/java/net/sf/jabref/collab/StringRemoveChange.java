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

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexString;
import net.sf.jabref.Globals;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableRemoveString;

public class StringRemoveChange extends Change {

  BibtexString string, inMem;

  InfoPane tp = new InfoPane();
  JScrollPane sp = new JScrollPane(tp);
    private BibtexString tmpString;


    public StringRemoveChange(BibtexString string, BibtexString tmpString, BibtexString inMem) {
        this.tmpString = tmpString;
        name = Globals.lang("Removed string")+": '"+string.getName()+"'";
    this.string = string;
    this.inMem = inMem; // Holds the version in memory. Check if it has been modified...?

        tp.setText("<HTML><H2>" + Globals.lang("Removed string") + "</H2><H3>" + Globals.lang("Label") + ":</H3>" + string.getName() + "<H3>" + Globals.lang("Content") + ":</H3>" + string.getContent() + "</HTML>");

  }

  public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {

    try {
      panel.database().removeString(inMem.getId());
      undoEdit.addEdit(new UndoableRemoveString(panel, panel.database(), string));
    } catch (Exception ex) {
      Globals.logger("Error: could not add string '"+string.getName()+"': "+ex.getMessage());
    }

      // Update tmp database:
      secondary.removeString(tmpString.getId());

      return true;
  }


  JComponent description() {
    return sp;
  }


}
