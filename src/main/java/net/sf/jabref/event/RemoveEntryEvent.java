package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Instances of this class are suitable for passing through the event bus.
 * A <code>BibEntry</code> object the changes were applied on is required.
 * <code>RemoveEntryEvent</code> should be used, when a <code>BibEntry</code>
 * is going to be removed from the database.
 */

public class RemoveEntryEvent {

    private final BibEntry bibEntry;

    public RemoveEntryEvent(BibEntry bibEntry) {
        this.bibEntry = bibEntry;
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

}
