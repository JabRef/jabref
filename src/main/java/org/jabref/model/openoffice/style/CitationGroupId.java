package org.jabref.model.openoffice.style;

/**
 * Identifies a citation group in a document.
 */
public class CitationGroupId {
    String id;
    public CitationGroupId(String id) {
        this.id = id;
    }

    /**
     * CitationEntry needs some string identifying the group that it can pass back later.
     */
    public String citationGroupIdAsString() {
        return id;
    }
}
