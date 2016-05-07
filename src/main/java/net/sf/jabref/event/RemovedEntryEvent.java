package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Instances of this class are suitable for passing through the event bus.
 * A <code>BibEntry</code> object the changes were applied on is required.
 * <code>RemovedEntryEvent</code> should be used, when a <code>BibEntry</code>
 * is going to be removed from the database.
 */

public class RemovedEntryEvent extends EntryEvent {

    public RemovedEntryEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

}
