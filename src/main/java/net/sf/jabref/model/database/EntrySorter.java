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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.jabref.event.AddEntryEvent;
import net.sf.jabref.event.ChangeEntryEvent;
import net.sf.jabref.event.RemoveEntryEvent;
import net.sf.jabref.model.entry.BibEntry;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EntrySorter {

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

    @Subscribe
    public void listen(AddEntryEvent aee) {
        synchronized (set) {
            int pos = -Collections.binarySearch(set, aee.getBibEntry(), comp) - 1;
            LOGGER.debug("Insert position = " + pos);
            if (pos >= 0) {
                set.add(pos, aee.getBibEntry());
            } else {
                set.add(0, aee.getBibEntry());
            }
        }
    }

    @Subscribe
    public void listen(RemoveEntryEvent ree) {
        synchronized (set) {
            set.remove(ree.getBibEntry());
            changed = true;
        }
    }

    @Subscribe
    public void listen(ChangeEntryEvent cee) {
        synchronized (set) {
            int pos = Collections.binarySearch(set, cee.getBibEntry(), comp);
            int posOld = set.indexOf(cee.getBibEntry());
            if (pos < 0) {
                set.remove(posOld);
                set.add(-posOld - 1, cee.getBibEntry());
            }
        }
    }

}
