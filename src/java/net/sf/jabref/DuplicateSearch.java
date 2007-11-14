/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

// created by : ?
//
// modified : r.nagel 2.09.2004
//            - new SearcherThread.setFinish() method
//            - replace thread.sleep in run() by wait() and notify() mechanism

package net.sf.jabref;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.SwingUtilities;

import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableRemoveEntry;
import spin.Spin;

public class DuplicateSearch extends Thread {

  BasePanel panel;
  BibtexEntry[] bes;
  final Vector<BibtexEntry[]> duplicates = new Vector<BibtexEntry[]>();
  boolean autoRemoveExactDuplicates = false;
  
  public DuplicateSearch(BasePanel bp) {
    panel = bp;
  }

public void run() {
  final NamedCompound ce = new NamedCompound(Globals.lang("duplicate removal"));
  int duplicateCounter = 0;
  
  autoRemoveExactDuplicates = false;
  panel.output(Globals.lang("Searching for duplicates..."));
  Object[] keys = panel.database.getKeySet().toArray();
  if ((keys == null) || (keys.length < 2))
    return;
  bes = new BibtexEntry[keys.length];
  for (int i=0; i<keys.length; i++)
    bes[i] = panel.database.getEntryById((String)keys[i]);

  SearcherThread st = new SearcherThread();
  st.setPriority(Thread.MIN_PRIORITY);
  st.start();
  int current = 0;

   final ArrayList<BibtexEntry> toRemove = new ArrayList<BibtexEntry>();
  while (!st.finished() || (current < duplicates.size()))
  {

    if (current >= duplicates.size() )
    {
      // wait until the search thread puts something into duplicates vector
      // or finish its work
      synchronized(duplicates)
      {
         try
         {
           duplicates.wait();
         }
         catch (Exception e) {}
      }
    } else  // duplicates found
    {


        BibtexEntry[] be = duplicates.get(current);
        current++;
        if (!toRemove.contains(be[0]) && !toRemove.contains(be[1])) {
            // Check if they are exact duplicates:
            boolean askAboutExact = false;
            if (DuplicateCheck.compareEntriesStrictly(be[0], be[1]) > 1) {
                if (autoRemoveExactDuplicates) {
                    toRemove.add(be[1]);
                    duplicateCounter++;
                    continue;
                } else {
                    askAboutExact = true;
                }
            }

            DuplicateCallBack cb = new DuplicateCallBack(panel.frame, be[0], be[1],
                    askAboutExact ? DuplicateResolverDialog.DUPLICATE_SEARCH_WITH_EXACT :
                            DuplicateResolverDialog.DUPLICATE_SEARCH);
            ((CallBack)(Spin.over(cb))).update();

            duplicateCounter++;
            int answer = cb.getSelected();
            if ((answer == DuplicateResolverDialog.KEEP_UPPER)
                    || (answer == DuplicateResolverDialog.AUTOREMOVE_EXACT)) {
                toRemove.add(be[1]);
                if (answer == DuplicateResolverDialog.AUTOREMOVE_EXACT)
                    autoRemoveExactDuplicates = true; // Remember choice
            } else if (answer == DuplicateResolverDialog.KEEP_LOWER) {
                toRemove.add(be[0]);
            } else if (answer == DuplicateResolverDialog.BREAK) {
                st.setFinished(); // thread killing
                current = Integer.MAX_VALUE;
                duplicateCounter--; // correct counter
            }
        }
    }
  }

    final int dupliC = duplicateCounter;
    SwingUtilities.invokeLater(new Runnable() {
        public void run() {
            // Now, do the actual removal:
            if (toRemove.size() > 0) {
                for (Iterator<BibtexEntry> iterator = toRemove.iterator(); iterator.hasNext();) {
                    BibtexEntry entry = iterator.next();
                    panel.database.removeEntry(entry.getId());
                    ce.addEdit(new UndoableRemoveEntry(panel.database, entry, panel));
                }
                panel.markBaseChanged();
            }
            panel.output(Globals.lang("Duplicate pairs found") + ": " + duplicates.size()
                       +" " +Globals.lang("pairs processed") +": " +dupliC );


                ce.end();
                panel.undoManager.addEdit(ce);

        }

    });


}


class SearcherThread extends Thread {

  private boolean finished = false;

  public void run() {
    for (int i = 0; (i < bes.length - 1) && !finished ; i++) {
      for (int j = i + 1; (j < bes.length) && !finished ; j++) {
        boolean eq = DuplicateCheck.isDuplicate(bes[i], bes[j]
        );

        // If (suspected) duplicates, add them to the duplicates vector.
        if (eq)
        {
          synchronized (duplicates)
          {
            duplicates.add( new BibtexEntry[] {bes[i], bes[j]} ) ;
            duplicates.notifyAll(); // send wake up all
          }
        }
      }
    }
    finished = true;

    // if no duplicates found, the graphical thread will never wake up
    synchronized(duplicates)
    {
      duplicates.notifyAll();
    }
  }

  public boolean finished() {
    return finished;
  }

  // Thread cancel option
  // no synchronized used because no "realy" critical situations expected
  public void setFinished()
  {
    finished = true ;
  }
}

    class DuplicateCallBack implements CallBack {
        private int reply = -1;
        DuplicateResolverDialog diag;
        private JabRefFrame frame;
        private BibtexEntry one;
        private BibtexEntry two;
        private int dialogType;

        public DuplicateCallBack(JabRefFrame frame, BibtexEntry one, BibtexEntry two,
                                 int dialogType) {

            this.frame = frame;
            this.one = one;
            this.two = two;
            this.dialogType = dialogType;
        }
        public int getSelected() {
            return reply;
        }
        public void update() {
            diag = new DuplicateResolverDialog(frame, one, two, dialogType);
            diag.setVisible(true);
            diag.dispose();
            reply = diag.getSelected();
        }
    }

}
