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

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;

public class EntryAddChange extends Change {

  BibtexEntry diskEntry;
//  boolean isModifiedLocally, modificationsAgree;[[[[[[
  PreviewPanel pp;
  JScrollPane sp;

  public EntryAddChange(BibtexEntry diskEntry) {
    super("Added entry");
    this.diskEntry = diskEntry;

    pp = new PreviewPanel(null, diskEntry, null, new MetaData(), Globals.prefs.get("preview0"));
    sp = new JScrollPane(pp);
  }

  public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {
      diskEntry.setId(Util.createNeutralId());
      panel.database().insertEntry(diskEntry);
      secondary.insertEntry(diskEntry);
      undoEdit.addEdit(new UndoableInsertEntry(panel.database(), diskEntry, panel));
      return true;
  }

  JComponent description() {
    return sp;
  }
}
