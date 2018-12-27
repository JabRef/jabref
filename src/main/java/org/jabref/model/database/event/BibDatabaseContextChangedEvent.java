package org.jabref.model.database.event;

import org.jabref.model.entry.event.EntryEvent;
import org.jabref.model.groups.event.GroupUpdatedEvent;
import org.jabref.model.metadata.event.MetaDataChangedEvent;

/**
 * This Event is automatically fired at the same time as {@link EntryEvent}, {@link GroupUpdatedEvent} or {@link MetaDataChangedEvent}.
 */
public class BibDatabaseContextChangedEvent {
    // no data
}
