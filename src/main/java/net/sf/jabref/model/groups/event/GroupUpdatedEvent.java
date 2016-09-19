package net.sf.jabref.model.groups.event;

import net.sf.jabref.model.metadata.MetaData;

public class GroupUpdatedEvent {

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
