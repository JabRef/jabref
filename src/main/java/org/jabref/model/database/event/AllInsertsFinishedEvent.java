package org.jabref.model.database.event;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryEvent;
import org.jabref.model.entry.event.EntryEventSource;

/**
 * {@link AllInsertsFinishedEvent} is fired when insertion of {@link BibEntry} to the {@link BibDatabase} was finished.
 */
public class AllInsertsFinishedEvent extends EntryEvent {

    /**
     * @param bibEntry the entry which has been added
     */
    public AllInsertsFinishedEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

    /**
     * @param bibEntry <code>BibEntry</code> object which has been added.
     * @param location Location affected by this event
     */
    public AllInsertsFinishedEvent(BibEntry bibEntry, EntryEventSource location) {
        super(bibEntry, location);
    }
}
