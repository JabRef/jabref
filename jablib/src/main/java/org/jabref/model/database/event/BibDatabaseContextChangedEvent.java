package org.jabref.model.database.event;

/// This event is automatically fired at the same time as {@link org.jabref.model.entry.event.EntriesEvent}, {@link org.jabref.model.groups.event.GroupUpdatedEvent}, or {@link org.jabref.model.metadata.event.MetaDataChangedEvent},
/// because all three inherit from this class.
public abstract class BibDatabaseContextChangedEvent {
    // If the event has been filtered out
    private boolean filteredOut;

    public BibDatabaseContextChangedEvent() {
        this(false);
    }

    public BibDatabaseContextChangedEvent(boolean filteredOut) {
        this.filteredOut = filteredOut;
    }

    /// Check if this event can be filtered out to be synchronized with a database at a later time.
    public boolean isFilteredOut() {
        return filteredOut;
    }

    public void setFilteredOut(boolean filtered) {
        this.filteredOut = filtered;
    }
}
