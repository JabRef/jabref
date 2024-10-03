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

    private final BibEntry firstEntry;

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
