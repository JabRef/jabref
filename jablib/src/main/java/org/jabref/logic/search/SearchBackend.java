package org.jabref.logic.search;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

/// **Implementor** role of the GoF Bridge pattern (cf. _Design Patterns: Elements
/// of Reusable Object-Oriented Software_, Gamma et al., 1994, pp. 151-161).
///
/// Concrete implementors plug into [SearchContext] (the Abstraction) and provide
/// the actual search + indexing behavior. Today: [org.jabref.logic.search.sqlbased.SqlSearchBackend]
/// (Postgres metadata index + Lucene linked-file index) and
/// [org.jabref.logic.search.inmemory.InMemorySearchBackend] (grammar walk
/// against in-memory entries, no fulltext).
public interface SearchBackend {

    /// Run the query against this backend.
    ///
    /// @return a [SearchResults] capturing matched-entry membership and (if supported) fulltext hits.
    SearchResults search(SearchQuery query);

    /// Test a single entry against the query without re-running a full library search.
    boolean isEntryMatched(BibEntry entry, SearchQuery query);

    /// Indexing lifecycle. Backends without an index (e.g. in-memory) implement these as no-ops.
    void addToIndex(List<BibEntry> entries);

    void removeFromIndex(List<BibEntry> entries);

    void updateEntry(FieldChangedEvent event);

    /// Re-index linked-file fulltext from scratch. Backends without a fulltext index implement as a no-op.
    void rebuildFullTextIndex();

    /// Release backend resources (DB connections, Lucene index handles, …). Idempotent.
    void close();
}
