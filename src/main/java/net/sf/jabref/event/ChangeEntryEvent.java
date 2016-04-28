package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Instances of this class are suitable for passing through the event bus.
 * A <code>BibEntry</code> object the changes were applied on is required.
 * <code>ChangeEntryEvent</code> should be used, when a <code>BibEntry</code>
 * was changed.
 */

public class ChangeEntryEvent extends EntryEvent {

    public ChangeEntryEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

}
