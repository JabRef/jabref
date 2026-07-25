package org.jabref.logic.search.query;

/// Pseudo-field names recognized by the Search.g4 grammar.
///
/// These are not real [org.jabref.model.entry.field.Field]s but query-language
/// aliases that every search visitor (SQL, Lucene, in-memory) handles specially.
/// Declared as compile-time constants so they can be used as `switch` case labels.
public final class SearchFieldConstants {

    /// Matches across all fields.
    public static final String ANY_FIELD = "any";

    /// Alias for [#ANY_FIELD].
    public static final String ANY_FIELD_ALIAS = "anyfield";

    /// Matches against the keywords field.
    public static final String ANY_KEYWORD = "anykeyword";

    /// Alias for the citation key field ("citationkey").
    public static final String KEY = "key";

    /// The citation key field.
    public static final String CITATION_KEY = "citationkey";

    /// The entry type.
    public static final String ENTRY_TYPE = "entrytype";

    private SearchFieldConstants() {
    }
}
