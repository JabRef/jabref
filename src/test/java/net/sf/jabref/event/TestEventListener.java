package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

import com.google.common.eventbus.Subscribe;

public class TestEventListener {

    private BibEntry bibEntry;


    @Subscribe
    public void listen(Event event) {
        this.bibEntry = (BibEntry) event.getObject();
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

}
