package org.jabref.model.database.event;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.entry.event.EntryEvent;
import org.jabref.model.entry.event.EntryEventSource;

import java.util.List;

/**
 * <code>RemovedEntryEvent</code> is fired when a <code>BibEntry</code> was removed
 * from the database.
 */

public class EntriesRemovedEvent extends EntriesEvent {

    /**
     * @param bibEntry <code>BibEntry</code> object which has been removed.
     */
    public EntriesRemovedEvent(List<BibEntry> bibEntries) {
        super(bibEntries);
    }

    /**
     * @param bibEntry <code>BibEntry</code> object which has been removed.
     * @param location Location affected by this event
     */
    public EntriesRemovedEvent(List<BibEntry> bibEntries, EntryEventSource location) {
        super(bibEntries, location);
    }

}
