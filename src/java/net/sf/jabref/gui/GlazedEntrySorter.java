package net.sf.jabref.gui;

import java.util.*;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.DatabaseChangeEvent;
import net.sf.jabref.DatabaseChangeListener;
import net.sf.jabref.IdComparator;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 12, 2005
 * Time: 8:54:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class GlazedEntrySorter implements DatabaseChangeListener {
/*
Copyright (C) 2003 Morten O. Alver

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

    //TreeSet list;
    EventList list;

    Comparator comp;
    String[] idArray;
    BibtexEntry[] entryArray;
    //static BibtexEntry[] DUMMY = new BibtexEntry[0];
    private boolean outdated = false;
    private boolean changed = false;

    public GlazedEntrySorter(Map entries, Comparator comp) {
        //list = new TreeSet(comp);
        list = new BasicEventList();
        //list2 = new SortedList(list, new FieldComparator(Globals.KEY_FIELD));
        this.comp = comp;
        list.getReadWriteLock().writeLock().lock();
        Set keySet = entries.keySet();
        if (keySet != null) {
            Iterator i = keySet.iterator();
            while (i.hasNext()) {
                list.add(entries.get(i.next()));
            }
        }

        // Sort the list so it is ordered according to creation (or read) order
        // when the table is unsorted.
        Collections.sort(list, new IdComparator());
        
        list.getReadWriteLock().writeLock().unlock();

    }

    public EventList getTheList() {
        return list;
    }

    public void databaseChanged(DatabaseChangeEvent e) {
        list.getReadWriteLock().writeLock().lock();
        if (e.getType() == DatabaseChangeEvent.ADDED_ENTRY) {
            //int pos = -Collections.binarySearch(list, e.getEntry(), comp) - 1;
            list.add(e.getEntry());
            //System.out.println("Added. Size: " + list.size());

        } else if (e.getType() == DatabaseChangeEvent.REMOVED_ENTRY) {
            list.remove(e.getEntry());
            //System.out.println("Removed. Size: " + list.size());
        } else if (e.getType() == DatabaseChangeEvent.CHANGED_ENTRY) {
            int index = list.indexOf(e.getEntry());
            list.set(index, e.getEntry());
        }
        list.getReadWriteLock().writeLock().unlock();

    }


}
