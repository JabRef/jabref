package org.jabref.http.dto;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/// Result for one query of a `POST /libraries/query` request.
///
/// `query` echoes the input expression; `matches` lists every matching entry
/// across all open libraries. Results appear in the same order as the input
/// queries.
@NullMarked
public record LibraryQueryResult(String query, List<LibraryQueryMatch> matches) {
}
