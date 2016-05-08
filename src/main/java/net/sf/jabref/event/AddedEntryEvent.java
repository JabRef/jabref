package net.sf.jabref.event;

import net.sf.jabref.model.entry.BibEntry;

/**
 * <code>AddedEntryEvent</code> is fired when a new <code>BibEntry</code> was added
 * to the database.
 */

public class AddedEntryEvent extends EntryEvent {

    /**
     * @param bibEntry <code>BibEntry</code> object which has been added.
     */
    public AddedEntryEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

}
