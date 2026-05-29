package org.jabref.logic.search;

import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

/// Inert [SearchBackend]: returns empty [SearchResults], matches nothing, and ignores
/// indexing calls. Used as a safe fallback when the expected backend is unavailable
/// so callers do not crash.
public class NoOpSearchBackend implements SearchBackend {

    @Override
    public SearchResults search(SearchQuery query) {
        return new SearchResults();
    }

    @Override
    public boolean isEntryMatched(BibEntry entry, SearchQuery query) {
        return false;
    }

    @Override
    public void addToIndex(List<BibEntry> entries) {
        // no-op
    }

    @Override
    public void removeFromIndex(List<BibEntry> entries) {
        // no-op
    }

    @Override
    public void updateEntry(FieldChangedEvent event) {
        // no-op
    }

    @Override
    public void rebuildFullTextIndex() {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
