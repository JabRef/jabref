package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Instances of this class are suitable for passing through the event bus.
 * A <code>BibEntry</code> object the changes were applied on is required.
 * <code>ChangedEntryEvent</code> should be used, when a <code>BibEntry</code>
 * was changed.
 */

public class ChangedEntryEvent extends EntryEvent {

    public ChangedEntryEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

}
