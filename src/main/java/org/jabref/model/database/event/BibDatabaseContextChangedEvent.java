package org.jabref.model.database.event;

import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.groups.event.GroupUpdatedEvent;
import org.jabref.model.metadata.event.MetaDataChangedEvent;

/**
 * This event is automatically fired at the same time as {@link EntriesEvent}, {@link GroupUpdatedEvent}, or {@link MetaDataChangedEvent},
 * because all three inherit from this class.
 */
public abstract class BibDatabaseContextChangedEvent {
    private boolean filtered = false;

    /**
     * Check if this event can be filtered out to be synchronized with a database at a later time.
     */
    public boolean isFiltered() {
        return filtered;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }
}
