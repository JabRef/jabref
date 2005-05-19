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

package net.sf.jabref;

import java.util.*;

public class EntrySorter implements DatabaseChangeListener {

    TreeSet set;
    String[] idArray;
    BibtexEntry[] entryArray;
    //static BibtexEntry[] DUMMY = new BibtexEntry[0];
    private boolean outdated = false;
    private boolean changing = false;

    public EntrySorter(Map entries, Comparator comp) {
	set = new TreeSet(comp);
	Set keySet = entries.keySet();
	if (keySet != null) {
	    Iterator i = keySet.iterator();
	    while (i.hasNext()) {
		set.add(entries.get(i.next()));
	    }
	    index();
	}
    }

    public void index() {

        while (changing) {
            // The boolean "changing" is true in the situation that an entry is about to change,
            // and has temporarily been removed from the entry set in this sorter. So, if we index
            // now, we will cause exceptions other places because one entry has been left out of
            // the indexed array. So we have no other choice than to wait for the entry to be readded.
            // The Thread.sleep() may not be a very good choice, but it should be safe.

            System.out.println("...changing...");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Nothing.
            }
            //Thread.dumpStack();
            //System.exit(0);
        }

        synchronized(set) {
            // Create an array of IDs for quick access, since getIdAt() is called by
            // getValueAt() in EntryTableModel, which *has* to be efficient.

	        int count = set.size();
            idArray = new String[count];
            entryArray = new BibtexEntry[count];
	        int piv = 0;
	        for (Iterator i=set.iterator(); i.hasNext();) {
	            //        for (int i=0; i<idArray.length; i++) {
    	        BibtexEntry entry = (BibtexEntry)i.next();
    	        idArray[piv] = entry.getId();
    	        entryArray[piv] = entry;
    	        piv++;
            }
        }
    }

    public boolean isOutdated() {
	return outdated;
    }

    public String getIdAt(int pos) {
        synchronized(set) {
            return idArray[pos];
        }
	//return ((BibtexEntry)(entryArray[pos])).getId();
    }

    public BibtexEntry getEntryAt(int pos) {
        synchronized(set) {
            return entryArray[pos];
        }
    }

    public int getEntryCount() {
        synchronized(set) {
	        if (entryArray != null)
	            return entryArray.length;
	        else
	        return 0;
        }
    }

    public void databaseChanged(DatabaseChangeEvent e) {
        synchronized(set) {
	        if (e.getType() == DatabaseChangeEvent.ADDED_ENTRY) {
	            set.add(e.getEntry());
	        }
	        else if (e.getType() == DatabaseChangeEvent.REMOVED_ENTRY) {
	            set.remove(e.getEntry());
	        }
	        else if (e.getType() == DatabaseChangeEvent.CHANGING_ENTRY) {
	            set.remove(e.getEntry());
                //System.out.println("CHANGING: "+e.getEntry().getCiteKey());
                //Thread.dumpStack();
                changing = true;
	        }
	        else if (e.getType() == DatabaseChangeEvent.CHANGED_ENTRY) {
	            // Remove and re-add the entry, to make sure it is in the
	            // correct sort position.
        	    set.add(e.getEntry());
                //System.out.println("CHANGED: "+e.getEntry().getCiteKey());
                changing = false;
            }

    	}

    }
}
