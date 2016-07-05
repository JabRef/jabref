package net.sf.jabref.event;

import net.sf.jabref.event.source.EntryEventSource;
import net.sf.jabref.model.entry.BibEntry;

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
