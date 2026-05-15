package org.jabref.http.dto;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Request body for `POST /libraries/query`.
///
/// `query` carries a raw Search.g4 expression and takes precedence over `dois` / `urls`
/// when present. The doi/url lists are a convenience that the server translates into a
/// Search.g4 expression internally.
/// All three fields are optional on the wire. `query` stays null if absent. The
/// list fields are normalized to an empty list so downstream code is free of
/// null checks; the accessors `dois()` and `urls()` therefore never return null.
@NullMarked
public record LibraryQueryRequest(@Nullable String query, List<String> dois, List<String> urls) {
    public LibraryQueryRequest {
        dois = dois != null ? dois : List.of();
        urls = urls != null ? urls : List.of();
    }
}
