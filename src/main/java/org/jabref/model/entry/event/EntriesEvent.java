package org.jabref.model.entry.event;

import java.util.List;
import java.util.Objects;

import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.entry.BibEntry;

/**
 * This abstract class pretends a minimal set of attributes and methods which an entries event should have.
 */
public abstract class EntriesEvent extends BibDatabaseContextChangedEvent {

    private final List<BibEntry> bibEntries;
    private final EntriesEventSource location;

    /**
     * @param bibEntries List of BibEntry objects which are involved in this event
     */
    public EntriesEvent(List<BibEntry> bibEntries) {
        this(bibEntries, EntriesEventSource.LOCAL);
    }

    /**
     * @param bibEntries List of BibEntry objects which are involved in this event
     * @param location   Location affected by this event
     */
    public EntriesEvent(List<BibEntry> bibEntries, EntriesEventSource location) {
        super();
        this.bibEntries = Objects.requireNonNull(bibEntries);
        this.location = Objects.requireNonNull(location);
    }

    public List<BibEntry> getBibEntries() {
        return this.bibEntries;
    }

    public EntriesEventSource getEntriesEventSource() {
        return this.location;
    }
}
