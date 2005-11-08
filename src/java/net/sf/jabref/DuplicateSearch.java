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

import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableRemoveEntry;
import java.util.Vector;

public class DuplicateSearch extends Thread {

  BasePanel panel;
  BibtexEntry[] bes;
  final Vector duplicates = new Vector();

  public DuplicateSearch(BasePanel bp) {
    panel = bp;
  }

public void run() {
  NamedCompound ce = null;
  int duplicateCounter = 0;
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
  DuplicateResolverDialog drd = null;

/*
  loop: while (!st.finished() || (current < duplicates.size()))
  {
    if ( current >= duplicates.size() )
    {
      // No more duplicates to resolve, but search is still in progress. Sleep a little.
       try
       {
         sleep(10);
       } catch (InterruptedException ex) {}
       continue loop;
    }
  }
*/

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
      BibtexEntry[] be = ( BibtexEntry[] ) duplicates.get( current ) ;
      current++ ;
      if ( ( panel.database.getEntryById( be[0].getId() ) != null ) &&
           ( panel.database.getEntryById( be[1].getId() ) != null ) )
      {

        drd = new DuplicateResolverDialog( panel.frame, be[0], be[1],
					   DuplicateResolverDialog.DUPLICATE_SEARCH) ;
        drd.show() ;

        duplicateCounter++ ;
        int answer = drd.getSelected() ;
        if ( answer == DuplicateResolverDialog.KEEP_UPPER )
        {
          if ( ce == null ) ce = new NamedCompound(Globals.lang("duplicate removal")) ;
          panel.database.removeEntry( be[1].getId() ) ;
          panel.markBaseChanged() ;
          ce.addEdit( new UndoableRemoveEntry( panel.database, be[1], panel ) ) ;
        }
        else if ( answer == DuplicateResolverDialog.KEEP_LOWER )
        {
          if ( ce == null ) ce = new NamedCompound(Globals.lang("duplicate removal")) ;
          panel.database.removeEntry( be[0].getId() ) ;
          panel.markBaseChanged() ;
          ce.addEdit( new UndoableRemoveEntry( panel.database, be[0], panel ) ) ;
        }
        else if ( answer == DuplicateResolverDialog.BREAK )
        {
          st.setFinished() ; // thread killing
          current = Integer.MAX_VALUE ;
          duplicateCounter-- ; // correct counter
        }
        drd.dispose();
      }
    }
  }

  if (drd != null)
    drd.dispose();

  panel.output(Globals.lang("Duplicate pairs found") + ": " + duplicates.size()
               +" " +Globals.lang("pairs processed") +": " +duplicateCounter );

  if (ce != null)
  {
    ce.end();
    //Util.pr("ox");
    panel.undoManager.addEdit(ce);
    //markBaseChanged();
    //refreshTable();
  }
}


class SearcherThread extends Thread {

  private boolean finished = false;

  public void run() {
    for (int i = 0; (i < bes.length - 1) && !finished ; i++) {
      for (int j = i + 1; (j < bes.length) && !finished ; j++) {
        boolean eq = Util.isDuplicate(bes[i], bes[j],
                                      Globals.duplicateThreshold);

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

}
