package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Instances of this class are suitable for passing through the event bus.
 * A <code>BibEntry</code> object the changes were applied on is required.
 * <code>ChangeEntryEvent</code> should be used, when a <code>BibEntry</code>
 * was changed.
 */

public class ChangeEntryEvent {

    private final BibEntry bibEntry;

    public ChangeEntryEvent(BibEntry bibEntry) {
        this.bibEntry = bibEntry;
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }

}
