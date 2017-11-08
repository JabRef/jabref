package org.jabref.logic.bibtex.comparator;

import java.util.Optional;

import org.jabref.model.metadata.MetaData;

public class MetaDataDiff {

    private final Optional<GroupDiff> groupDiff;
    private MetaData newMetaData;

    private MetaDataDiff(MetaData originalMetaData, MetaData newMetaData) {
        this.newMetaData = newMetaData;
        this.groupDiff = GroupDiff.compare(originalMetaData, newMetaData);
    }

    public static Optional<MetaDataDiff> compare(MetaData originalMetaData, MetaData newMetaData) {
        if (originalMetaData.equals(newMetaData)) {
            return Optional.empty();
        } else {
            return Optional.of(new MetaDataDiff(originalMetaData, newMetaData));
        }
    }

    public MetaData getNewMetaData() {
        return newMetaData;
    }

    public Optional<GroupDiff> getGroupDifferences() {
        return groupDiff;
    }
}
