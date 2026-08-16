package org.jabref.logic.search.inmemory;

import java.util.EnumSet;
import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.search.SearchBackend;
import org.jabref.logic.search.sqlbased.LinkedFilesIndexManager;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import org.jspecify.annotations.NullMarked;

/// A search backend that evaluates metadata in memory and searches linked files with Lucene.
// [impl->req~jabgui.search.fulltext.lucene-without-postgres~1]
@NullMarked
public class InMemoryLuceneSearchBackend implements SearchBackend {

    private final InMemorySearchBackend inMemorySearchBackend;
    private final LinkedFilesIndexManager linkedFilesIndexManager;

    public InMemoryLuceneSearchBackend(BibDatabaseContext databaseContext,
                                       BibEntryPreferences bibEntryPreferences,
                                       FilePreferences filePreferences,
                                       TaskExecutor taskExecutor) {
        inMemorySearchBackend = new InMemorySearchBackend(databaseContext, bibEntryPreferences);
        linkedFilesIndexManager = new LinkedFilesIndexManager(databaseContext, taskExecutor, filePreferences);
    }

    @Override
    public SearchResults search(SearchQuery query) {
        SearchResults searchResults = inMemorySearchBackend.search(createMetadataQuery(query));
        if (query.getSearchFlags().contains(SearchFlags.FULLTEXT)) {
            searchResults.mergeSearchResults(linkedFilesIndexManager.search(query));
        }
        query.setSearchResults(searchResults);
        return searchResults;
    }

    private static SearchQuery createMetadataQuery(SearchQuery query) {
        EnumSet<SearchFlags> metadataFlags = EnumSet.copyOf(query.getSearchFlags());
        metadataFlags.remove(SearchFlags.FULLTEXT);
        return new SearchQuery(query.getSearchExpression(), metadataFlags);
    }

    @Override
    public boolean isEntryMatched(BibEntry entry, SearchQuery query) {
        return inMemorySearchBackend.isEntryMatched(entry, query);
    }

    @Override
    public void addToIndex(List<BibEntry> entries) {
        linkedFilesIndexManager.addToIndex(entries);
    }

    @Override
    public void removeFromIndex(List<BibEntry> entries) {
        linkedFilesIndexManager.removeFromIndex(entries);
    }

    @Override
    public void updateEntry(FieldChangedEvent event) {
        linkedFilesIndexManager.updateEntry(event);
    }

    @Override
    public void rebuildFullTextIndex() {
        linkedFilesIndexManager.rebuildIndex();
    }

    @Override
    public void close() {
        linkedFilesIndexManager.closeAndWait();
    }
}
