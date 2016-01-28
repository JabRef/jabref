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
package net.sf.jabref.gui;

import java.util.Collections;
import java.util.Map;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.database.DatabaseChangeEvent;
import net.sf.jabref.model.database.DatabaseChangeListener;
import net.sf.jabref.logic.id.IdComparator;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class GlazedEntrySorter implements DatabaseChangeListener {

    private final EventList<BibEntry> list;

    public GlazedEntrySorter(Map<String, BibEntry> entries) {
        list = new BasicEventList<>();
        list.getReadWriteLock().writeLock().lock();
        for (Map.Entry<String, BibEntry> entry : entries.entrySet()) {
            list.add(entry.getValue());
        }

        // Sort the list so it is ordered according to creation (or read) order
        // when the table is unsorted.
        Collections.sort(list, new IdComparator());

        list.getReadWriteLock().writeLock().unlock();

    }

    public EventList<BibEntry> getTheList() {
        return list;
    }

    @Override
    public void databaseChanged(DatabaseChangeEvent e) {
        list.getReadWriteLock().writeLock().lock();
        if (e.getType() == DatabaseChangeEvent.ChangeType.ADDED_ENTRY) {
            list.add(e.getEntry());
        } else if (e.getType() == DatabaseChangeEvent.ChangeType.REMOVED_ENTRY) {
            list.remove(e.getEntry());
        } else if (e.getType() == DatabaseChangeEvent.ChangeType.CHANGED_ENTRY) {
            int index = list.indexOf(e.getEntry());
            if (index != -1) {
                // SpecialFieldUtils.syncSpecialFieldsFromKeywords update an entry during
                // DatabaseChangeEvent.ADDED_ENTRY
                // thus,
                list.set(index, e.getEntry());
            }
        }
        list.getReadWriteLock().writeLock().unlock();

    }

}
