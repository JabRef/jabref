package org.jabref.logic.bibtex.comparator;

import java.util.StringJoiner;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// A `null` component means the entry only exists on the other side.
@NullMarked
public record BibEntryDiff(
        @Nullable BibEntry originalEntry,
        @Nullable BibEntry newEntry) {

    @Override
    public String toString() {
        return new StringJoiner(",\n", BibEntryDiff.class.getSimpleName() + "[", "]")
                .add("originalEntry=" + originalEntry)
                .add("newEntry=" + newEntry)
                .toString();
    }
}
