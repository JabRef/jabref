package org.jabref.http.dto;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/// Request body for `POST /libraries/query`.
///
/// `queries` is an ordered list of raw Search.g4 expressions. Each query is run
/// independently against all open libraries; the position of a query is preserved
/// in the response so callers can map, e.g., the n-th reference of a web page to
/// the n-th query result.
///
/// The list is normalized to an empty list so downstream code is free of null
/// checks; the accessor `queries()` therefore never returns null.
@NullMarked
public record LibraryQueryRequest(List<String> queries) {
    public LibraryQueryRequest {
        queries = queries != null ? queries : List.of();
    }
}
