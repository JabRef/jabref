package org.jabref.logic.bibtex.comparator;

import java.util.StringJoiner;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record BibEntryDiff(
        @Nullable BibEntry originalEntry,
        @Nullable BibEntry newEntry
) {

    @Override
    public @NonNull String toString() {
        return new StringJoiner(",\n", BibEntryDiff.class.getSimpleName() + "[", "]")
                .add("originalEntry=" + originalEntry)
                .add("newEntry=" + newEntry)
                .toString();
    }
}
