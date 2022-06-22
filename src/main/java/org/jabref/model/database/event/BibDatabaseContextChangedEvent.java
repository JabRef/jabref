package org.jabref.model.database.event;

import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.groups.event.GroupUpdatedEvent;
import org.jabref.model.metadata.event.MetaDataChangedEvent;

/**
 * This Event is automatically fired at the same time as {@link EntriesEvent}, {@link GroupUpdatedEvent} or {@link MetaDataChangedEvent}.
 */
public class BibDatabaseContextChangedEvent {
    // If the event has been filtered out
    private boolean filteredOut;

    public BibDatabaseContextChangedEvent() {
        this(false);
    }

    public BibDatabaseContextChangedEvent(boolean filteredOut) {
        this.filteredOut = filteredOut;
    }

    /**
     * Check if this event can be filtered out to be synchronized with a database at a later time.
     */
    public boolean isFilteredOut() {
        return filteredOut;
    }

    public void setFilteredOut(boolean filtered) {
        this.filteredOut = filtered;
    }
}
