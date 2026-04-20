package org.jabref.http.dto;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record LibraryQueryMatch(@Nullable String doi, @Nullable String url, String libraryId, String entryId) {
    public static LibraryQueryMatch forDoi(String doi, String libraryId, String entryId) {
        return new LibraryQueryMatch(doi, null, libraryId, entryId);
    }

    public static LibraryQueryMatch forUrl(String url, String libraryId, String entryId) {
        return new LibraryQueryMatch(null, url, libraryId, entryId);
    }
}
