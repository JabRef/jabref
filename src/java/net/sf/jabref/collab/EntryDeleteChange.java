package net.sf.jabref.collab;

import net.sf.jabref.Globals;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BasePanel;
import net.sf.jabref.Util;
import net.sf.jabref.KeyCollisionException;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.sf.jabref.PreviewPanel;
import javax.swing.JScrollPane;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableRemoveEntry;

public class EntryDeleteChange extends Change {

  BibtexEntry memEntry, tmpEntry, diskEntry;
  boolean isModifiedLocally;
  double matchWithTmp;
  PreviewPanel pp;
  JScrollPane sp;

  public EntryDeleteChange(BibtexEntry memEntry, BibtexEntry tmpEntry) {
    super("Deleted entry");
    this.memEntry = memEntry;
    this.tmpEntry = tmpEntry;

    // Compare the deleted entry in memory with the one in the tmpfile. The
    // entry could have been removed in memory.
    matchWithTmp = Util.compareEntriesStrictly(memEntry, tmpEntry);

    // Check if it has been modified locally, since last tempfile was saved.
    isModifiedLocally = !(matchWithTmp > 1);

    //Util.pr("Modified entry: "+memEntry.getCiteKey()+"\n Modified locally: "+isModifiedLocally
    //        +" Modifications agree: "+modificationsAgree);

    pp = new PreviewPanel(memEntry, Globals.prefs.get("preview0"));
    sp = new JScrollPane(pp);
  }

  public void makeChange(BasePanel panel, NamedCompound undoEdit) {
    panel.database().removeEntry(memEntry.getId());
    undoEdit.addEdit(new UndoableRemoveEntry(panel.database(), memEntry, panel));
  }

  JComponent description() {
    return sp;
  }
}
