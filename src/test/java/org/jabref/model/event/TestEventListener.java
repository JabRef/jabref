package org.jabref.model.event;

import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.database.event.EntryAddedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryChangedEvent;

import com.google.common.eventbus.Subscribe;

public class TestEventListener {

    private BibEntry bibEntry;

    @Subscribe
    public void listen(EntryAddedEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    // Not sure if this will work
    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        this.bibEntry = event.getBibEntries().get(0);
    }

    @Subscribe
    public void listen(EntryChangedEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }
}
