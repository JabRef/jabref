package org.jabref.model.entry.event;

import java.util.Objects;

import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.entry.BibEntry;

/**
 * This abstract class pretends a minimal set of attributes and methods
 * which an entry event should have.
 */
public abstract class EntryEvent extends BibDatabaseContextChangedEvent {

    private final BibEntry bibEntry;
    private final EntryEventSource location;


    /**
     * @param bibEntry BibEntry object which is involved in this event
     */
    public EntryEvent(BibEntry bibEntry) {
        this(bibEntry, EntryEventSource.LOCAL);
    }

    /**
     * @param bibEntry BibEntry object which is involved in this event
     * @param location Location affected by this event
     */
    public EntryEvent(BibEntry bibEntry, EntryEventSource location) {
        this.bibEntry = Objects.requireNonNull(bibEntry);
        this.location = Objects.requireNonNull(location);
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

    public EntryEventSource getEntryEventSource() {
        return this.location;
    }
}
