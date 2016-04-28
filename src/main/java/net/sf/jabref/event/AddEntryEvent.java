package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Instances of this class are suitable for passing through the event bus.
 * A <code>BibEntry</code> object the changes were applied on is required.
 * <code>AddEntryEvent</code> should be used, when new <code>BibEntry</code>
 * was added to the database.
 */

public class AddEntryEvent extends EntryEvent {

    public AddEntryEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

}
