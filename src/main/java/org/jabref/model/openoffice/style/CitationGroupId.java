package org.jabref.model.openoffice.style;

/**
 * Identifies a citation group in a document.
 */
public class CitationGroupId {
    String groupId;
    public CitationGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * CitationEntry needs some string identifying the group that it can pass back later.
     */
    public String citationGroupIdAsString() {
        return groupId;
    }
}
