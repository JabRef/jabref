package org.jabref.http.dto;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/// Response body for `POST /libraries:query`.
///
/// `results` holds one [LibraryQueryResult] per input query, in the same order.
@NullMarked
public record LibraryQueryResponse(List<LibraryQueryResult> results) {
}
