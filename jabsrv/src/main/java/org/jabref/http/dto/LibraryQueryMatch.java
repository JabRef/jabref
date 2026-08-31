package org.jabref.http.dto;

import org.jspecify.annotations.NullMarked;

/// A single entry that matched a query, identified by its library and citation key.
///
/// `entryId` is the entry's citation key. Because citation keys are optional in the
/// underlying model, entries without a key are reported with the sentinel
/// [#UNSET_CITATION_KEY] (`{NONE}`) instead of an empty string. The sentinel uses
/// curly braces deliberately: `{` and `}` are reserved in the BibTeX grammar
/// (group delimiters in field values) and can never appear inside a real citation
/// key, so the sentinel is guaranteed not to collide with a valid key. Other
/// "obvious" markers such as `<NONE>` are *not* safe — Pandoc, for example, allows
/// `<` and `>` in citation keys.
///
/// Callers cannot distinguish multiple unkeyed entries from one another via this
/// DTO — fix the library by assigning citation keys if disambiguation is required.
@NullMarked
public record LibraryQueryMatch(String libraryId, String entryId) {
    public static final String UNSET_CITATION_KEY = "{NONE}";
}
