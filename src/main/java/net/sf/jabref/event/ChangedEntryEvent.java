package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * <code>ChangedEntryEvent</code> is fired when a <code>BibEntry</code> has been changed.
 */

public class ChangedEntryEvent extends EntryEvent {

    /**
     * @param bibEntry <code>BibEntry</code> object the changes were applied on.
     */
    public ChangedEntryEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

}
