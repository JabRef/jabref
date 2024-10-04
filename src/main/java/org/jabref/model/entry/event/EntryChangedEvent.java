package org.jabref.model.entry.event;

import java.util.Collections;

import org.jabref.model.entry.BibEntry;

/**
 * <code>EntryChangedEvent</code> is fired when a <code>BibEntry</code> has been changed.
 */

public class EntryChangedEvent extends EntriesEvent {

    /**
     * @param bibEntry where the changes were applied on.
     */
    public EntryChangedEvent(BibEntry bibEntry) {
        super(Collections.singletonList(bibEntry));
    }

    /**
     * @param bibEntry where the changes were applied on.
     * @param source Source of this event
     */
    public EntryChangedEvent(BibEntry bibEntry, EntriesEventSource source) {
        super(Collections.singletonList(bibEntry), source);
    }

    public BibEntry getBibEntry() {
        // An entryChangedEvent should only have one BibEntry, but its parent class stores a List<BibEntry>
        return getBibEntries().getFirst();
    }
}
