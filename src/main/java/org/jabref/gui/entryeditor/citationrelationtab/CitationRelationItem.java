package org.jabref.gui.entryeditor.citationrelationtab;

import org.jabref.model.entry.BibEntry;

/**
 * Class to hold a BibEntry and a boolean value whether it is already in the current database or not.
 */
public class CitationRelationItem {
    private final BibEntry entry;
    private final boolean isLocal;

    public CitationRelationItem(BibEntry entry, boolean isLocal) {
        this.entry = entry;
        this.isLocal = isLocal;
    }

    public BibEntry getEntry() {
        return entry;
    }

    public boolean isLocal() {
        return isLocal;
    }
}
