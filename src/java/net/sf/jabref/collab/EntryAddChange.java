package net.sf.jabref.collab;

import net.sf.jabref.Globals;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BasePanel;
import net.sf.jabref.Util;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableInsertEntry;
import net.sf.jabref.KeyCollisionException;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.sf.jabref.PreviewPanel;
import javax.swing.JScrollPane;

public class EntryAddChange extends Change {

  BibtexEntry diskEntry;
//  boolean isModifiedLocally, modificationsAgree;[[[[[[
  PreviewPanel pp;
  JScrollPane sp;

  public EntryAddChange(BibtexEntry diskEntry) {
    super("Added entry");
    this.diskEntry = diskEntry;

    pp = new PreviewPanel(diskEntry, Globals.prefs.get("preview0"));
    sp = new JScrollPane(pp);
  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {
    try {
      diskEntry.setId(Util.createNeutralId());
      panel.database().insertEntry(diskEntry);
      undoEdit.addEdit(new UndoableInsertEntry(panel.database(), diskEntry, panel));
    } catch (KeyCollisionException ex) {

    }

  }

  JComponent description() {
    return sp;
  }
}
