/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.model.database;

import net.sf.jabref.model.entry.BibEntry;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EntrySorter implements DatabaseChangeListener {

    private static final Log LOGGER = LogFactory.getLog(EntrySorter.class);

    private final List<BibEntry> set;
    private final Comparator<BibEntry> comp;
    private BibEntry[] entryArray;
    private boolean changed;


    public EntrySorter(List<BibEntry> entries, Comparator<BibEntry> comp) {
        set = entries;
        this.comp = comp;
        changed = true;
        index();
    }

    private void index() {

        /*  Old version, from when set was a TreeSet.

        // The boolean "changing" is true in the situation that an entry is about to change,
        // and has temporarily been removed from the entry set in this sorter. So, if we index
        // now, we will cause exceptions other places because one entry has been left out of
        // the indexed array. Simply waiting foth this to change can lead to deadlocks,
        // so we have no other choice than to return without indexing.
        if (changing)
            return;
        */

        synchronized (set) {

            // Resort if necessary:
            if (changed) {
                Collections.sort(set, comp);
                changed = false;
            }

            // Create an array of IDs for quick access, since getIdAt() is called by
            // getValueAt() in EntryTableModel, which *has* to be efficient.

            int count = set.size();
            entryArray = new BibEntry[count];
            int piv = 0;
            for (BibEntry entry : set) {
                entryArray[piv] = entry;
                piv++;
            }
        }
    }

    public BibEntry getEntryAt(int pos) {
        synchronized (set) {
            return entryArray[pos];
        }
    }

    public int getEntryCount() {
        synchronized (set) {
            if (entryArray != null) {
                return entryArray.length;
            } else {
                return 0;
            }
        }
    }

    @Override
    public void databaseChanged(DatabaseChangeEvent e) {
        synchronized (set) {
            int pos;
            switch (e.getType()) {
            case ADDED_ENTRY:
                pos = -Collections.binarySearch(set, e.getEntry(), comp) - 1;
                LOGGER.debug("Insert position = " + pos);
                if (pos >= 0) {
                    set.add(pos, e.getEntry());
                } else {
                    set.add(0, e.getEntry());
                }
                break;
            case REMOVED_ENTRY:
                set.remove(e.getEntry());
                changed = true;
                break;
            case CHANGED_ENTRY:
                pos = Collections.binarySearch(set, e.getEntry(), comp);
                int posOld = set.indexOf(e.getEntry());
                if (pos < 0) {
                    set.remove(posOld);
                    set.add(-posOld - 1, e.getEntry());
                }
                break;
            default:
                break;
            }
        }
    }
}
