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

    pp = new PreviewPanel(diskEntry, new MetaData(), Globals.prefs.get("preview0"));
    sp = new JScrollPane(pp);
  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {
      diskEntry.setId(Util.createNeutralId());
      panel.database().insertEntry(diskEntry);
      undoEdit.addEdit(new UndoableInsertEntry(panel.database(), diskEntry, panel));
  }

  JComponent description() {
    return sp;
  }
}
