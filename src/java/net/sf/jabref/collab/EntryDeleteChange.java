package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.sf.jabref.*;
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
    matchWithTmp = DuplicateCheck.compareEntriesStrictly(memEntry, tmpEntry);

    // Check if it has been modified locally, since last tempfile was saved.
    isModifiedLocally = !(matchWithTmp > 1);

    //Util.pr("Modified entry: "+memEntry.getCiteKey()+"\n Modified locally: "+isModifiedLocally
    //        +" Modifications agree: "+modificationsAgree);

    pp = new PreviewPanel(null, memEntry, null, new MetaData(), Globals.prefs.get("preview0"));
    sp = new JScrollPane(pp);
  }

  public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {
    panel.database().removeEntry(memEntry.getId());
    undoEdit.addEdit(new UndoableRemoveEntry(panel.database(), memEntry, panel));
    secondary.removeEntry(tmpEntry.getId());
    return true;
  }

  JComponent description() {
    return sp;
  }
}
