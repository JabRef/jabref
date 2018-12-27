package org.jabref.model.metadata.event;

import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.metadata.MetaData;

/**
 * {@link MetaDataChangedEvent} is fired when a tuple of meta data has been put or removed.
 */
public class MetaDataChangedEvent extends BibDatabaseContextChangedEvent {

    private final MetaData metaData;

    /**
     * @param metaData Affected instance
     */
    public MetaDataChangedEvent(MetaData metaData) {
        this.metaData = metaData;
    }

    public MetaData getMetaData() {
        return this.metaData;
    }
}
