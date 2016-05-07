package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Instances of this class are suitable for passing through the event bus.
 * A <code>BibEntry</code> object the changes were applied on is required.
 * <code>AddedEntryEvent</code> should be used, when new <code>BibEntry</code>
 * was added to the database.
 */

public class AddedEntryEvent extends EntryEvent {

    public AddedEntryEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

}
