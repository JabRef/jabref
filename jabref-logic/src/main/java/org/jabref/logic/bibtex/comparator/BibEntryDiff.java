package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibEntry;

public class BibEntryDiff {
    private final BibEntry originalEntry;
    private final BibEntry newEntry;

    public BibEntryDiff(BibEntry originalEntry, BibEntry newEntry) {
        this.originalEntry = originalEntry;
        this.newEntry = newEntry;
    }

    public BibEntry getOriginalEntry() {
        return originalEntry;
    }

    public BibEntry getNewEntry() {
        return newEntry;
    }
}
