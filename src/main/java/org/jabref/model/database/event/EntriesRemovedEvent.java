package org.jabref.model.database.event;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.entry.event.EntriesEventSource;

/**
 * <code>EntriesRemovedEvent</code> is fired when at least one {@link BibEntry} is being removed
 * from the database.
 */

public class EntriesRemovedEvent extends EntriesEvent {
    public EntriesRemovedEvent(List<BibEntry> bibEntries, EntriesEventSource location) {
        super(bibEntries, location);
    }
}
