package org.jabref.logic.search.inmemory;

import java.util.List;

import org.jabref.logic.search.SearchBackend;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResult;
import org.jabref.model.search.query.SearchResults;

/// Concrete **Implementor** ([SearchBackend]) backed by [InMemoryLibrarySearcher].
///
/// The in-memory grammar walk only matches metadata; it produces no fulltext
/// (linked-file content) results. Lifecycle calls are no-ops because there is
/// no persistent index to maintain.
public class InMemorySearchBackend implements SearchBackend {

    private final InMemoryLibrarySearcher searcher;

    public InMemorySearchBackend(BibDatabaseContext databaseContext, BibEntryPreferences bibEntryPreferences) {
        this.searcher = new InMemoryLibrarySearcher(databaseContext, bibEntryPreferences);
    }

    @Override
    public SearchResults search(SearchQuery query) {
        SearchResults results = new SearchResults();
        SearchResult marker = new SearchResult();
        for (BibEntry entry : searcher.getMatches(query)) {
            results.addSearchResult(entry.getId(), marker);
        }
        query.setSearchResults(results);
        return results;
    }

    @Override
    public boolean isEntryMatched(BibEntry entry, SearchQuery query) {
        return searcher.matches(entry, query);
    }

    @Override
    public void addToIndex(List<BibEntry> entries) {
        // no index to maintain
    }

    @Override
    public void removeFromIndex(List<BibEntry> entries) {
        // no index to maintain
    }

    @Override
    public void updateEntry(FieldChangedEvent event) {
        // no index to maintain
    }

    @Override
    public void rebuildFullTextIndex() {
        // no fulltext index to rebuild
    }

    @Override
    public void close() {
        // no resources to release
    }
}
