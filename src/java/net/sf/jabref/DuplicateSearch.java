package net.sf.jabref;

import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableRemoveEntry;
import java.util.Vector;

public class DuplicateSearch extends Thread {

  BasePanel panel;
  BibtexEntry[] bes;
  Vector duplicates = new Vector();

  public DuplicateSearch(BasePanel bp) {
    panel = bp;
  }

public void run() {
  NamedCompound ce = null;
  int dupl = 0;
  panel.output(Globals.lang("Searching for duplicates..."));
  Object[] keys = panel.database.getKeySet().toArray();
  if ((keys == null) || (keys.length < 2))
    return;
  bes = new BibtexEntry[keys.length];
  for (int i=0; i<keys.length; i++)
    bes[i] = panel.database.getEntryById((String)keys[i]);

  SearcherThread st = new SearcherThread();
  st.start();
  int current = 0;
  DuplicateResolverDialog drd = null;

  loop: while (!st.finished() || (current < duplicates.size())) {
    if (current >= duplicates.size()) {
      // No more duplicates to resolve, but search is still in progress. Sleep a little.
      try {
        sleep(10);
      } catch (InterruptedException ex) {}
      continue loop;
    }

    BibtexEntry[] be = (BibtexEntry[])duplicates.get(current);
    current++;
    if ((panel.database.getEntryById(be[0].getId()) != null) &&
        (panel.database.getEntryById(be[1].getId()) != null)) {


      if (drd == null) {
        drd = new DuplicateResolverDialog(panel.frame, be[0], be[1]);
        drd.show();
      }
      else
        drd.setEntries(be[0], be[1]);

      while (drd.isBlocking()) {
        try {
          sleep(100);
        } catch (InterruptedException ex) {}
      }


      int answer = drd.getSelected();
      if (answer == DuplicateResolverDialog.KEEP_UPPER) {
        if (ce == null) ce = new NamedCompound("duplicate removal");
        panel.database.removeEntry(be[1].getId());
        panel.refreshTable();
        panel.markBaseChanged();
        ce.addEdit(new UndoableRemoveEntry(panel.database, be[1], panel));
      }
      else if (answer == DuplicateResolverDialog.KEEP_LOWER) {
        if (ce == null) ce = new NamedCompound("duplicate removal");
        panel.database.removeEntry(be[0].getId());
        panel.refreshTable();
        panel.markBaseChanged();
        ce.addEdit(new UndoableRemoveEntry(panel.database, be[0], panel));
      }
      dupl++;
      //Util.pr("---------------------------------------------------");
      //Util.pr("--> "+i+" and "+j+" ...");
      //Util.pr("---------------------------------------------------");

    }
    drd.dispose();
  }





  if (drd != null)
    drd.dispose();
  panel.output(Globals.lang("Duplicate pairs found") + ": " + dupl);

  if (ce != null) {
    ce.end();
    //Util.pr("ox");
    panel.undoManager.addEdit(ce);
    //markBaseChanged();
    //refreshTable();
  }
}

class SearcherThread extends Thread {

  boolean finished = false;

  public void run() {
    for (int i = 0; i < bes.length - 1; i++) {
      for (int j = i + 1; j < bes.length; j++) {
        boolean eq = Util.isDuplicate(bes[i], bes[j],
                                      Globals.duplicateThreshold);

        // If (suspected) duplicates, add them to the duplicates vector.
        if (eq) {
          duplicates.add(new BibtexEntry[] {bes[i], bes[j]});
        }
      }
    }
    finished = true;
  }

  public boolean finished() {
    return finished;
  }
}


}
