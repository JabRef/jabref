package org.jabref.model.event;

import org.jabref.model.database.event.AllInsertsFinishedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.database.event.EntryAddedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryChangedEvent;

import com.google.common.eventbus.Subscribe;

public class TestEventListener {

    private BibEntry addedEntry;
    private BibEntry firstInsertedEntry;
    private BibEntry removedEntry;
    private BibEntry changedEntry;

    @Subscribe
    public void listen(EntryAddedEvent event) {
        this.addedEntry = event.getBibEntry();
    }

    @Subscribe
    public void listen(AllInsertsFinishedEvent event) {
        this.firstInsertedEntry = event.getBibEntry();
    }

    // Not sure if this will work
    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        this.removedEntry = event.getBibEntries().get(0);
    }

    @Subscribe
    public void listen(EntryChangedEvent event) {
        this.changedEntry = event.getBibEntry();
    }

    public BibEntry getAddedEntry() {
        return addedEntry;
    }

    public BibEntry getFirstInsertedEntry() {
        return firstInsertedEntry;
    }

    public BibEntry getRemovedEntry() {
        return removedEntry;
    }

    public BibEntry getChangedEntry() {
        return changedEntry;
    }
}
