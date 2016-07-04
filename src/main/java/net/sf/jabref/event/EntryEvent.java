package net.sf.jabref.event;

import net.sf.jabref.event.scope.EntryEventScope;
import net.sf.jabref.model.entry.BibEntry;

/**
 * This abstract class pretends a minimal set of attributes and methods
 * which an entry event should have.
 */
public abstract class EntryEvent {

    private final BibEntry bibEntry;
    private final EntryEventScope location;


    /**
     * @param bibEntry BibEntry object which is involved in this event
     */
    public EntryEvent(BibEntry bibEntry) {
        this(bibEntry, EntryEventScope.LOCAL_AND_REMOTE);
    }

    /**
     * @param bibEntry BibEntry object which is involved in this event
     * @param location Location affected by this event
     */
    public EntryEvent(BibEntry bibEntry, EntryEventScope location) {
        this.bibEntry = bibEntry;
        this.location = location;
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

    public EntryEventScope getEntryEventLocation() {
        return this.location;
    }
}
