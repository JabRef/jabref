package org.jabref.model.openoffice.style;

/**
 * Identifies a citation with the identifier of the citation group containing it and its storage
 * index within.
 */
public class CitationPath {

    public final CitationGroupId group;

    public final int storageIndexInGroup;

    CitationPath(CitationGroupId group, int storageIndexInGroup) {
        this.group = group;
        this.storageIndexInGroup = storageIndexInGroup;
    }
}
