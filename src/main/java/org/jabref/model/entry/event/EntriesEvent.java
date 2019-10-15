package org.jabref.model.entry.event;

import java.util.List;
import java.util.Objects;

import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.entry.BibEntry;

/**
 * This abstract class pretends a minimal set of attributes and methods
 * which an entry event should have.
 */
public abstract class EntriesEvent extends BibDatabaseContextChangedEvent {

    private final List<BibEntry> bibEntries;
    private final EntryEventSource location;


    /**
     * @param bibEntry BibEntry object which is involved in this event
     */
    public EntriesEvent(List<BibEntry> bibEntries) {
        this(bibEntries, EntryEventSource.LOCAL);
    }

    /**
     * @param bibEntry BibEntry object which is involved in this event
     * @param location Location affected by this event
     */
    public EntriesEvent(List<BibEntry> bibEntries, EntryEventSource location) {
        this.bibEntries = Objects.requireNonNull(bibEntries);
        this.location = Objects.requireNonNull(location);
    }

    //Temporary, while we change to plural entries
    public BibEntry getBibEntry() { return this.bibEntries.get(0); }

    public List<BibEntry> getBibEntries() {
        return this.bibEntries;
    }

    public EntryEventSource getEntryEventSource() {
        return this.location;
    }
}
