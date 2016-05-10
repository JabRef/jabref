package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * This abstract class pretends a minimal set of attributes and methods
 * which an entry event should have.
 */
public abstract class EntryEvent {

    private final BibEntry bibEntry;

    /**
     * @param bibEntry BibEntry object which is involved in this event 
     */
    public EntryEvent(BibEntry bibEntry) {
        this.bibEntry = bibEntry;
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }
}
