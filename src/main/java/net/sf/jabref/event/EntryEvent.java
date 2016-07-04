package net.sf.jabref.event;

import net.sf.jabref.event.location.EntryEventTargetScope;
import net.sf.jabref.model.entry.BibEntry;

/**
 * This abstract class pretends a minimal set of attributes and methods
 * which an entry event should have.
 */
public abstract class EntryEvent {

    private final BibEntry bibEntry;
    private final EntryEventTargetScope location;


    /**
     * @param bibEntry BibEntry object which is involved in this event
     */
    public EntryEvent(BibEntry bibEntry) {
        this(bibEntry, EntryEventTargetScope.LOCAL_AND_REMOTE);
    }

    /**
     * @param bibEntry BibEntry object which is involved in this event
     * @param location Location affected by this event
     */
    public EntryEvent(BibEntry bibEntry, EntryEventTargetScope location) {
        this.bibEntry = bibEntry;
        this.location = location;
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

    public EntryEventTargetScope getEntryEventLocation() {
        return this.location;
    }
}
