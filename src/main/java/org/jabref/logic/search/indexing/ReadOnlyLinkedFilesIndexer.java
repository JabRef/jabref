package org.jabref.logic.search.indexing;

import java.io.IOException;
import java.util.Collection;

import org.jabref.logic.search.LuceneIndexer;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadOnlyLinkedFilesIndexer implements LuceneIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadOnlyLinkedFilesIndexer.class);
    private Directory indexDirectory;
    private SearcherManager searcherManager;

    public ReadOnlyLinkedFilesIndexer(BibDatabaseContext databaseContext) {
        try {
            indexDirectory = FSDirectory.open(databaseContext.getFulltextIndexPath());
            searcherManager = new SearcherManager(indexDirectory, null);
        } catch (IOException e) {
            LOGGER.error("Error initializing read only linked files index", e);
        }
    }

    @Override
    public void updateOnStart(BackgroundTask<?> task) {
    }

    @Override
    public void addToIndex(Collection<BibEntry> entries, BackgroundTask<?> task) {
    }

    @Override
    public void removeFromIndex(Collection<BibEntry> entries, BackgroundTask<?> task) {
    }

    @Override
    public void updateEntry(BibEntry entry, String oldValue, String newValue, BackgroundTask<?> task) {
    }

    @Override
    public void removeAllFromIndex() {
    }

    @Override
    public void rebuildIndex(BackgroundTask<?> task) {
    }

    @Override
    public SearcherManager getSearcherManager() {
        return searcherManager;
    }

    @Override
    public void close() {
        HeadlessExecutorService.INSTANCE.execute(this::closeIndex);
    }

    @Override
    public void closeAndWait() {
        HeadlessExecutorService.INSTANCE.executeAndWait(this::closeIndex);
    }

    private void closeIndex() {
        try {
            searcherManager.close();
            indexDirectory.close();
        } catch (IOException e) {
            LOGGER.error("Error closing index", e);
        }
    }
}
