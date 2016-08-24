package net.sf.jabref.event;

import net.sf.jabref.MetaData;

/**
 * {@link MetaDataChangedEvent} is fired when a tuple of meta data has been put or removed.
 */
public class MetaDataChangedEvent {

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
