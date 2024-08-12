package org.jabref.logic.search.indexing;

import java.io.IOException;
import java.util.Collection;

import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadOnlyLinkedFilesIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadOnlyLinkedFilesIndexer.class);
    private Directory indexDirectory;
    private SearcherManager searcherManager;
    private IndexSearcher indexSearcher;

    public ReadOnlyLinkedFilesIndexer(BibDatabaseContext databaseContext) {
        try {
            indexDirectory = FSDirectory.open(databaseContext.getFulltextIndexPath());
            searcherManager = new SearcherManager(indexDirectory, null);
        } catch (IOException e) {
            LOGGER.error("Error initializing read only linked files index", e);
        }
    }

//    @Override
    public void updateOnStart() {
    }

//    @Override
    public void addToIndex(Collection<BibEntry> entries) {
    }

//    @Override
    public void removeFromIndex(Collection<BibEntry> entries) {
    }

//    @Override
    public void updateEntry(BibEntry entry, String oldValue, String newValue) {
    }

//    @Override
    public void removeAllFromIndex() {
    }

//    @Override
    public void rebuildIndex() {
    }

//    @Override
    public IndexSearcher getIndexSearcher() {
        try {
            if (indexSearcher != null) {
                searcherManager.release(indexSearcher);
            }
            searcherManager.maybeRefresh();
            indexSearcher = searcherManager.acquire();
        } catch (IOException e) {
            LOGGER.error("Error refreshing searcher", e);
        }
        return indexSearcher;
    }

//    @Override
    public void close() {
        HeadlessExecutorService.INSTANCE.execute(() -> {
            try {
                searcherManager.close();
                indexDirectory.close();
            } catch (IOException e) {
                LOGGER.error("Error closing index", e);
            }
        });
    }
}
