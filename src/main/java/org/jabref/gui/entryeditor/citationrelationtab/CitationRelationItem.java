package org.jabref.gui.entryeditor.citationrelationtab;

import org.jabref.model.entry.BibEntry;

/**
 * Class to hold a BibEntry and a boolean value whether it is already in the current database or not.
 */
public record CitationRelationItem(
        BibEntry entry,
        boolean isLocal) {
}
