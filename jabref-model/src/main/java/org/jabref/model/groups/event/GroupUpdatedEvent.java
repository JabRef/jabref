package org.jabref.model.groups.event;

import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.metadata.MetaData;

public class GroupUpdatedEvent extends BibDatabaseContextChangedEvent {

    private final MetaData metaData;

    /**
     * @param metaData Affected instance
     */
    public GroupUpdatedEvent(MetaData metaData) {
        this.metaData = metaData;
    }

    public MetaData getMetaData() {
        return this.metaData;
    }
}
