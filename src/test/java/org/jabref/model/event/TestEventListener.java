package org.jabref.model.event;

import org.jabref.model.database.event.EntryAddedEvent;
import org.jabref.model.database.event.EntryRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryChangedEvent;

import com.google.common.eventbus.Subscribe;

public class TestEventListener {

    private BibEntry bibEntry;


    @Subscribe
    public void listen(EntryAddedEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    @Subscribe
    public void listen(EntryRemovedEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    @Subscribe
    public void listen(EntryChangedEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

}
