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
package net.sf.jabref.gui.maintable;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.event.EntryAddedEvent;
import net.sf.jabref.model.event.EntryChangedEvent;
import net.sf.jabref.model.event.EntryRemovedEvent;

import ca.odell.glazedlists.EventList;
import com.google.common.eventbus.Subscribe;

public class ListSynchronizer {

    private final EventList<BibEntry> list;

    public ListSynchronizer(EventList<BibEntry> list) {
        this.list = list;
    }

    @Subscribe
    public void listen(EntryAddedEvent entryAddedEvent) {
        lock();
        try {
            list.add(entryAddedEvent.getBibEntry());
        } finally {
            unlock();
        }
    }

    @Subscribe
    public void listen(EntryRemovedEvent entryRemovedEvent) {
        lock();
        try {
            list.remove(entryRemovedEvent.getBibEntry());
        } finally {
            unlock();
        }
    }

    @Subscribe
    public void listen(EntryChangedEvent entryChangedEvent) {
        lock();
        try {
            int index = list.indexOf(entryChangedEvent.getBibEntry());
            if (index != -1) {
                // SpecialFieldUtils.syncSpecialFieldsFromKeywords update an entry during
                // DatabaseChangeEvent.ADDED_ENTRY
                // thus,
                list.set(index, entryChangedEvent.getBibEntry());
            }
        } finally {
            unlock();
        }
    }

    private void lock() {
        list.getReadWriteLock().writeLock().lock();
    }

    private void unlock() {
        list.getReadWriteLock().writeLock().unlock();
    }
}
