package org.jabref.model.entry.event;

import java.util.Collections;

import org.jabref.model.entry.BibEntry;

/**
 * <code>EntryChangedEvent</code> is fired when a <code>BibEntry</code> has been changed.
 */

public class EntryChangedEvent extends EntriesEvent {

    /**
     * @param bibEntry <code>BibEntry</code> object the changes were applied on.
     */
    public EntryChangedEvent(BibEntry bibEntry) {
        super(Collections.singletonList(bibEntry));
    }

    /**
     * @param bibEntry <code>BibEntry</code> object the changes were applied on.
     * @param location Location affected by this event
     */
    public EntryChangedEvent(BibEntry bibEntry, EntriesEventSource location) {
        super(Collections.singletonList(bibEntry), location);
    }

    public BibEntry getBibEntry() {
        // An entryChangedEvent should only have one BibEntry, but its parent class stores a List<BibEntry>
        return getBibEntries().get(0);
    }
}
