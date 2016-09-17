package net.sf.jabref.model.database.event;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.event.EntryEvent;
import net.sf.jabref.model.entry.event.EntryEventSource;

/**
 * {@link EntryAddedEvent} is fired when a new {@link BibEntry} was added to the {@link BibDatabase}.
 */
public class EntryAddedEvent extends EntryEvent {

    /**
     * @param bibEntry the entry which has been added
     */
    public EntryAddedEvent(BibEntry bibEntry) {
        super(bibEntry);
    }

    /**
     * @param bibEntry <code>BibEntry</code> object which has been added.
     * @param location Location affected by this event
     */
    public EntryAddedEvent(BibEntry bibEntry, EntryEventSource location) {
        super(bibEntry, location);
    }
}
