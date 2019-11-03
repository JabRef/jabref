package org.jabref.model.database.event;

import java.util.Collections;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.entry.event.EntriesEventSource;

/**
 * {@link EntryAddedEvent} is fired when a new {@link BibEntry} was added to the {@link BibDatabase}.
 */
public class EntryAddedEvent extends EntriesEvent {

    /**
     * @param bibEntry the entry which has been added
     */
    public EntryAddedEvent(BibEntry bibEntry) {
        super(Collections.singletonList(bibEntry));
    }

    /**
     * @param bibEntry <code>BibEntry</code> object which has been added.
     * @param location Location affected by this event
     */
    public EntryAddedEvent(BibEntry bibEntry, EntriesEventSource location) {
        super(Collections.singletonList(bibEntry), location);
    }
}
