package org.jabref.model.database.event;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.entry.event.EntriesEventSource;

/**
 * <code>EntriesRemovedEvent</code> is fired when at least one <code>BibEntry</code> is being removed
 * from the database.
 */

public class EntriesRemovedEvent extends EntriesEvent {

    /**
     * @param bibEntries <code>List</code> of <code>BibEntry</code> objects which are being removed.
     */
    public EntriesRemovedEvent(List<BibEntry> bibEntries) {
        super(bibEntries);
    }

    /**
     * @param bibEntries <code>List</code> of <code>BibEntry</code> objects which are being removed.
     * @param location   Location affected by this event
     */
    public EntriesRemovedEvent(List<BibEntry> bibEntries, EntriesEventSource location) {
        super(bibEntries, location);
    }
}
