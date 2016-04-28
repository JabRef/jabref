package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

public abstract class EntryEvent {

    private final BibEntry bibEntry;

    public EntryEvent(BibEntry bibEntry) {
        this.bibEntry = bibEntry;
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }
}
