package org.jabref.gui.maintable;

import org.jabref.model.database.event.EntryAddedEvent;
import org.jabref.model.database.event.EntryRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryChangedEvent;

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
            // cannot use list#indexOf b/c it won't distinguish between duplicates
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) == entryChangedEvent.getBibEntry()) {
                    list.set(i, entryChangedEvent.getBibEntry());
                    break;
                }
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
