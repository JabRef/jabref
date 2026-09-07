package org.jabref.model.database.event;

/// This event is automatically fired at the same time as [org.jabref.model.entry.event.EntriesEvent], [org.jabref.model.groups.event.GroupUpdatedEvent], or [org.jabref.model.metadata.event.MetaDataChangedEvent],
/// because all three inherit from this class.
public abstract class BibDatabaseContextChangedEvent {
    private boolean filtered = false;

    /// Check if this event can be filtered out to be synchronized with a database at a later time.
    public boolean isFiltered() {
        return filtered;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }
}
