package org.jabref.gui.openoffice;

/**
 * Identifies a citation group in a document.
 */
class CitationGroupID {
    String id;
    CitationGroupID(String id) {
        this.id = id;
    }

    /**
     *  CitationEntry needs refMark or other identifying string
     */
    String asString() {
        return id;
    }
}
