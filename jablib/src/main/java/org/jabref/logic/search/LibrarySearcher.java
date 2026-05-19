package org.jabref.logic.search;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;

/// Searches a library for entries matching a given [SearchQuery].
///
/// Implementations differ in their backing search engine
/// (e.g. SQL/Lucene-backed indexed search vs. in-memory grammar walk).
public interface LibrarySearcher {

    /// @return The matches in the order they appear in the library.
    List<BibEntry> getMatches(SearchQuery query);
}
