package org.jabref.model.database.event;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.entry.event.EntriesEventSource;

/// {@link EntriesAddedEvent} is fired when at least {@link BibEntry} is being added to the {@link org.jabref.model.database.BibDatabase}.
public class EntriesAddedEvent extends EntriesEvent {

    // firstEntry used by listeners that used to listen to AllInsertsFinishedEvent
    // final?
    private final BibEntry firstEntry;

    /// @param bibEntries `List` of `BibEntry` objects which are being added.
    /// @param location   Location affected by this event
    public EntriesAddedEvent(List<BibEntry> bibEntries, EntriesEventSource location) {
        super(bibEntries, location);

        // The event makes only sense if there is at least one entry
        assert !bibEntries.isEmpty();

        this.firstEntry = bibEntries.getFirst();
    }

    public BibEntry getFirstEntry() {
        return this.firstEntry;
    }
}
