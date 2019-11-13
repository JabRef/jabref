package org.jabref.model.database.event;

import java.util.Collections;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.entry.event.EntriesEventSource;

/**
 * {@link AllInsertsFinishedEvent} is fired when insertion of {@link BibEntry} to the {@link BibDatabase} was finished.
 */
public class AllInsertsFinishedEvent extends EntriesEvent {

    /**
     * @param bibEntry the entry which has been added
     */
    public AllInsertsFinishedEvent(BibEntry bibEntry) {
        super(Collections.singletonList(bibEntry));
    }

    /**
     * @param bibEntry <code>BibEntry</code> object which has been added.
     * @param location Location affected by this event
     */
    public AllInsertsFinishedEvent(BibEntry bibEntry, EntriesEventSource location) {
        super(Collections.singletonList(bibEntry), location);
    }
}
