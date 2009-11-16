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
