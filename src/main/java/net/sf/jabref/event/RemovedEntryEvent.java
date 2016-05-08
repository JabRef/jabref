package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * <code>RemovedEntryEvent</code> is fired when a <code>BibEntry</code> was removed
 * from the database.
 */

public class RemovedEntryEvent extends EntryEvent {

    /**
     * @param bibEntry <code>BibEntry</code> object which has been removed.
     */
    public RemovedEntryEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

}
