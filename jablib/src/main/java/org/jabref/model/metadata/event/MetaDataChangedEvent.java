package org.jabref.model.metadata.event;

import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.metadata.MetaData;

/// [MetaDataChangedEvent] is fired when a tuple of metadata has been put or removed.
public class MetaDataChangedEvent extends BibDatabaseContextChangedEvent {

    private final MetaData metaData;
    private final MetaDataChangeSource source;

    /// @param metaData Affected instance
    /// @param source   who is behind the change — see [MetaDataChangeSource]
    public MetaDataChangedEvent(MetaData metaData, MetaDataChangeSource source) {
        super();
        this.metaData = metaData;
        this.source = source;
    }

    public MetaData getMetaData() {
        return this.metaData;
    }

    public MetaDataChangeSource getSource() {
        return this.source;
    }
}
