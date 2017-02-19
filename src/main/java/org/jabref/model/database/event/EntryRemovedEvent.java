package org.jabref.model.database.event;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryEvent;
import org.jabref.model.entry.event.EntryEventSource;

/**
 * <code>RemovedEntryEvent</code> is fired when a <code>BibEntry</code> was removed
 * from the database.
 */

public class EntryRemovedEvent extends EntryEvent {

    /**
     * @param bibEntry <code>BibEntry</code> object which has been removed.
     */
    public EntryRemovedEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

    /**
     * @param bibEntry <code>BibEntry</code> object which has been removed.
     * @param location Location affected by this event
     */
    public EntryRemovedEvent(BibEntry bibEntry, EntryEventSource location) {
        super(bibEntry, location);
    }

}
