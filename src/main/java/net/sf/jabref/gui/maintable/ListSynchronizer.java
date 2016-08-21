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
