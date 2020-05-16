package org.jabref.model.database.event;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.entry.event.EntriesEventSource;

/**
 * {@link EntriesAddedEvent} is fired when at least {@link BibEntry} is being added to the {@link BibDatabase}.
 */
public class EntriesAddedEvent extends EntriesEvent {

    // firstEntry used by listeners that used to listen to AllInsertsFinishedEvent
    // final?
    private final BibEntry firstEntry;

    /**
     * @param bibEntries the entries which are being added
     * @param firstEntry the first entry being added
     */

    public EntriesAddedEvent(List<BibEntry> bibEntries, BibEntry firstEntry, EntriesEventSource location) {
        super(bibEntries, location);
        this.firstEntry = firstEntry;
    }

    /**
     * @param bibEntries <code>List</code> of <code>BibEntry</code> objects which are being added.
     * @param location   Location affected by this event
     */
    public EntriesAddedEvent(List<BibEntry> bibEntries, EntriesEventSource location) {
        super(bibEntries, location);
        this.firstEntry = null;
    }

    public BibEntry getFirstEntry() {
        return this.firstEntry;
    }
}
