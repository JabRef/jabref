package org.jabref.model.entry.event;

import java.util.List;
import java.util.Objects;

import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.entry.BibEntry;

/**
 * This abstract class pretends a minimal set of attributes and methods
 * which an entries event should have.
 */
public abstract class EntriesEvent extends BibDatabaseContextChangedEvent {

    private final List<BibEntry> bibEntries;
    private final EntriesEventSource location;


    /**
     * @param List of bibEntry BibEntry objects which are involved in this event
     */
    public EntriesEvent(List<BibEntry> bibEntries) {
        this(bibEntries, EntriesEventSource.LOCAL);
    }

    /**
     * @param List of bibEntry BibEntry objects which are involved in this event
     * @param location Location affected by this event
     */
    public EntriesEvent(List<BibEntry> bibEntries, EntriesEventSource location) {
        this.bibEntries = Objects.requireNonNull(bibEntries);
        this.location = Objects.requireNonNull(location);
    }

    //Temporary fix, while we change to plural entries
    public BibEntry getBibEntry() { return this.bibEntries.get(0); }

    public List<BibEntry> getBibEntries() {
        return this.bibEntries;
    }

    public EntriesEventSource getEntriesEventSource() {
        return this.location;
    }
}
