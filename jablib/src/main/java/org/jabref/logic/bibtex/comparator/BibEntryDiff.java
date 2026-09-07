package org.jabref.logic.bibtex.comparator;

import java.util.StringJoiner;

import org.jabref.model.entry.BibEntry;

public record BibEntryDiff(
        BibEntry originalEntry,
        BibEntry newEntry) {

    @Override
    public String toString() {
        return new StringJoiner(",\n", BibEntryDiff.class.getSimpleName() + "[", "]")
                .add("originalEntry=" + originalEntry)
                .add("newEntry=" + newEntry)
                .toString();
    }
}
