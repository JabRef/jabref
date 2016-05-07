package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

import com.google.common.eventbus.Subscribe;

public class TestEventListener {

    private BibEntry bibEntry;


    @Subscribe
    public void listen(AddedEntryEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    @Subscribe
    public void listen(RemovedEntryEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    @Subscribe
    public void listen(ChangedEntryEvent event) {
        this.bibEntry = event.getBibEntry();
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

}
