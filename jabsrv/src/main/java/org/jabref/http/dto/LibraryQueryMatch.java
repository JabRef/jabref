package org.jabref.http.dto;

import org.jspecify.annotations.NullMarked;

/// A single entry that matched a query, identified by its library and citation key.
@NullMarked
public record LibraryQueryMatch(String libraryId, String entryId) {
}
