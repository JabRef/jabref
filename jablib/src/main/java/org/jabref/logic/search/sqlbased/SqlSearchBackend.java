package org.jabref.logic.search.sqlbased;

import java.util.List;

import org.jabref.logic.search.SearchBackend;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

/// Concrete **Implementor** ([SearchBackend]) backed by [IndexManager]
/// (Postgres metadata index + Lucene linked-file index).
public class SqlSearchBackend implements SearchBackend {

    private final IndexManager indexManager;

    public SqlSearchBackend(IndexManager indexManager) {
        this.indexManager = indexManager;
    }

    @Override
    public SearchResults search(SearchQuery query) {
        return indexManager.search(query);
    }

    @Override
    public boolean isEntryMatched(BibEntry entry, SearchQuery query) {
        return indexManager.isEntryMatched(entry, query);
    }

    @Override
    public void addToIndex(List<BibEntry> entries) {
        indexManager.addToIndex(entries);
    }

    @Override
    public void removeFromIndex(List<BibEntry> entries) {
        indexManager.removeFromIndex(entries);
    }

    @Override
    public void updateEntry(FieldChangedEvent event) {
        indexManager.updateEntry(event);
    }

    @Override
    public void rebuildFullTextIndex() {
        indexManager.rebuildFullTextIndex();
    }

    @Override
    public void close() {
        indexManager.closeAndWait();
    }
}
