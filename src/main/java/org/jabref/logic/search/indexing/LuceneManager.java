package org.jabref.logic.search.indexing;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.search.retrieval.LuceneSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.LuceneIndexer;
import org.jabref.model.search.LuceneSearchResults;
import org.jabref.model.search.SearchFlags;
import org.jabref.preferences.PreferencesService;

import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuceneManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneManager.class);

    private final BibDatabaseContext databaseContext;
    private final TaskExecutor taskExecutor;
    private final PreferencesService preferences;
    private final BooleanProperty shouldIndexLinkedFiles;
    private final BooleanProperty isLinkedFilesIndexerBlocked;
    private final ChangeListener<Boolean> preferencesListener;
    private final LuceneSearcher luceneSearcher;
    private LuceneIndexer linkedFilesIndexer;
    private LuceneIndexer bibFieldsIndexer;

    public LuceneManager(BibDatabaseContext databaseContext, TaskExecutor executor, PreferencesService preferences) {
        this.databaseContext = databaseContext;
        this.taskExecutor = executor;
        this.preferences = preferences;
        this.shouldIndexLinkedFiles = preferences.getFilePreferences().fulltextIndexLinkedFilesProperty();
        this.preferencesListener = (observable, oldValue, newValue) -> bindToPreferences(newValue);
        this.shouldIndexLinkedFiles.addListener(preferencesListener);
        this.isLinkedFilesIndexerBlocked = new SimpleBooleanProperty(false);
        this.luceneSearcher = new LuceneSearcher(databaseContext);

        try {
            bibFieldsIndexer = new BibFieldsIndexer(databaseContext, executor, preferences);
        } catch (IOException e) {
            LOGGER.error("Error initializing bib fields index", e);
        }

        initializeLinkedFilesIndexer();
        if (!shouldIndexLinkedFiles.get()) {
            linkedFilesIndexer.removeAllFromIndex();
            linkedFilesIndexer.close();
            linkedFilesIndexer = null;
        }
    }

    private void bindToPreferences(boolean newValue) {
        if (newValue) {
            initializeLinkedFilesIndexer();
            linkedFilesIndexer.updateOnStart();
        } else {
            linkedFilesIndexer.removeAllFromIndex();
            linkedFilesIndexer.close();
            linkedFilesIndexer = null;
        }
    }

    private void initializeLinkedFilesIndexer() {
        try {
            linkedFilesIndexer = new DefaultLinkedFilesIndexer(databaseContext, taskExecutor, preferences);
        } catch (IOException e) {
            LOGGER.info("Error initializing linked files index - using read only index");
            linkedFilesIndexer = new ReadOnlyLinkedFilesIndexer(databaseContext);
        }
    }

    public void updateOnStart() {
        bibFieldsIndexer.updateOnStart();
        if (shouldIndexLinkedFiles.get()) {
            linkedFilesIndexer.updateOnStart();
        }
    }

    public void addToIndex(Collection<BibEntry> entries) {
        bibFieldsIndexer.addToIndex(entries);
        if (shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
            linkedFilesIndexer.addToIndex(entries);
        }
    }

    public void removeFromIndex(Collection<BibEntry> entries) {
        bibFieldsIndexer.removeFromIndex(entries);
        if (shouldIndexLinkedFiles.get()) {
            linkedFilesIndexer.removeFromIndex(entries);
        }
    }

    public void updateEntry(BibEntry entry, String oldValue, String newValue, boolean isLinkedFile) {
        bibFieldsIndexer.updateEntry(entry, oldValue, newValue);
        if (isLinkedFile && shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
            linkedFilesIndexer.updateEntry(entry, oldValue, newValue);
        }
    }

    public void updateAfterDropFiles(BibEntry entry) {
        bibFieldsIndexer.updateEntry(entry, "", "");
        if (shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
            linkedFilesIndexer.addToIndex(List.of(entry));
        }
    }

    public void rebuildIndex() {
        bibFieldsIndexer.rebuildIndex();
        if (shouldIndexLinkedFiles.get()) {
            linkedFilesIndexer.rebuildIndex();
        }
    }

    public void close() {
        bibFieldsIndexer.close();
        shouldIndexLinkedFiles.removeListener(preferencesListener);
        if (linkedFilesIndexer != null) {
            linkedFilesIndexer.close();
        }
    }

    public AutoCloseable blockLinkedFileIndexer() {
        isLinkedFilesIndexerBlocked.set(true);
        return () -> isLinkedFilesIndexerBlocked.set(false);
    }

    public IndexSearcher getIndexSearcher(SearchQuery query) {
        if (query.getSearchFlags().contains(SearchFlags.FULLTEXT) && shouldIndexLinkedFiles.get()) {
            try {
                MultiReader reader = new MultiReader(bibFieldsIndexer.getIndexSearcher().getIndexReader(), linkedFilesIndexer.getIndexSearcher().getIndexReader());
                return new IndexSearcher(reader);
            } catch (IOException e) {
                LOGGER.error("Error getting index searcher", e);
                return bibFieldsIndexer.getIndexSearcher();
            }
        } else {
            return bibFieldsIndexer.getIndexSearcher();
        }
    }

    public HashMap<BibEntry, LuceneSearchResults> search(SearchQuery query) {
        return luceneSearcher.search(query, getIndexSearcher(query));
    }
}
