package org.jabref.model.event;

import java.util.List;

import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryChangedEvent;

import com.google.common.eventbus.Subscribe;

public class TestEventListener {

    private List<BibEntry> addedEntries;
    private BibEntry firstInsertedEntry;
    private List<BibEntry> removedEntries;
    private BibEntry changedEntry;

    @Subscribe
    public void listen(EntriesAddedEvent event) {
        this.addedEntries = event.getBibEntries();
        this.firstInsertedEntry = event.getFirstEntry();
    }

    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        this.removedEntries = event.getBibEntries();
    }

    @Subscribe
    public void listen(EntryChangedEvent event) {
        this.changedEntry = event.getBibEntry();
    }

    public List<BibEntry> getAddedEntries() {
        return addedEntries;
    }

    public BibEntry getFirstInsertedEntry() {
        return firstInsertedEntry;
    }

    public List<BibEntry> getRemovedEntries() {
        return removedEntries;
    }

    public BibEntry getChangedEntry() {
        return changedEntry;
    }
}
