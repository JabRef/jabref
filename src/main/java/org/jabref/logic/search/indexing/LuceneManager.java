package org.jabref.logic.search.indexing;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

import org.jabref.gui.util.BackgroundTask;
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
    private static final Map<String, LuceneManager> LUCENE_MANAGERS = new ConcurrentHashMap<>();

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
        LUCENE_MANAGERS.put(databaseContext.getUid(), this);
        this.databaseContext = databaseContext;
        this.taskExecutor = executor;
        this.preferences = preferences;
        this.isLinkedFilesIndexerBlocked = new SimpleBooleanProperty(false);
        this.shouldIndexLinkedFiles = preferences.getFilePreferences().fulltextIndexLinkedFilesProperty();
        this.preferencesListener = (observable, oldValue, newValue) -> bindToPreferences(newValue);
        this.shouldIndexLinkedFiles.addListener(preferencesListener);
        this.luceneSearcher = new LuceneSearcher(databaseContext);

        initializeIndexers();
    }

    public static LuceneManager get(BibDatabaseContext databaseContext) {
        return LUCENE_MANAGERS.get(databaseContext.getUid());
    }

    private void initializeIndexers() {
        try {
            bibFieldsIndexer = new BibFieldsIndexer(databaseContext, taskExecutor, preferences);
        } catch (IOException e) {
            LOGGER.error("Error initializing bib fields index", e);
        }
        initializeLinkedFilesIndexer();
        if (!shouldIndexLinkedFiles.get() && linkedFilesIndexer != null) {
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

    private void bindToPreferences(boolean newValue) {
        BackgroundTask.wrap(() -> {
            if (newValue) {
                initializeLinkedFilesIndexer();
                linkedFilesIndexer.updateOnStart();
            } else {
                linkedFilesIndexer.removeAllFromIndex();
                linkedFilesIndexer.close();
                linkedFilesIndexer = null;
            }
        }).executeWith(taskExecutor);
    }

    public void updateOnStart() {
        BackgroundTask.wrap(bibFieldsIndexer::updateOnStart).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get()) {
            BackgroundTask.wrap(linkedFilesIndexer::updateOnStart).executeWith(taskExecutor);
        }
    }

    public void addToIndex(Collection<BibEntry> entries) {
        BackgroundTask.wrap(() -> bibFieldsIndexer.addToIndex(entries)).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
            BackgroundTask.wrap(() -> linkedFilesIndexer.addToIndex(entries)).executeWith(taskExecutor);
        }
    }

    public void removeFromIndex(Collection<BibEntry> entries) {
        BackgroundTask.wrap(() -> bibFieldsIndexer.removeFromIndex(entries)).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get()) {
            BackgroundTask.wrap(() -> linkedFilesIndexer.removeFromIndex(entries)).executeWith(taskExecutor);
        }
    }

    public void updateEntry(BibEntry entry, String oldValue, String newValue, boolean isLinkedFile) {
        BackgroundTask.wrap(() -> bibFieldsIndexer.updateEntry(entry, oldValue, newValue)).executeWith(taskExecutor);

        if (isLinkedFile && shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
            BackgroundTask.wrap(() -> linkedFilesIndexer.updateEntry(entry, oldValue, newValue)).executeWith(taskExecutor);
        }
    }

    public void updateAfterDropFiles(BibEntry entry) {
        BackgroundTask.wrap(() -> bibFieldsIndexer.updateEntry(entry, "", "")).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
            BackgroundTask.wrap(() -> linkedFilesIndexer.addToIndex(List.of(entry))).executeWith(taskExecutor);
        }
    }

    public void rebuildIndex() {
        BackgroundTask.wrap(bibFieldsIndexer::rebuildIndex).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get()) {
            BackgroundTask.wrap(linkedFilesIndexer::rebuildIndex).executeWith(taskExecutor);
        }
    }

    public void close() {
        bibFieldsIndexer.close();
        shouldIndexLinkedFiles.removeListener(preferencesListener);
        if (linkedFilesIndexer != null) {
            linkedFilesIndexer.close();
        }
        LUCENE_MANAGERS.remove(databaseContext.getUid());
    }

    public AutoCloseable blockLinkedFileIndexer() {
        LOGGER.info("Blocking linked files indexer");
        isLinkedFilesIndexerBlocked.set(true);
        return () -> isLinkedFilesIndexerBlocked.set(false);
    }

    public IndexSearcher getIndexSearcher(SearchQuery query) {
        if (query.getSearchFlags().contains(SearchFlags.FULLTEXT) && shouldIndexLinkedFiles.get()) {
            try (MultiReader reader = new MultiReader(bibFieldsIndexer.getIndexSearcher().getIndexReader(), linkedFilesIndexer.getIndexSearcher().getIndexReader())) {
                return new IndexSearcher(reader);
            } catch (IOException e) {
                LOGGER.error("Error getting index searcher", e);
            }
        }
        return bibFieldsIndexer.getIndexSearcher();
    }

    public HashMap<BibEntry, LuceneSearchResults> search(SearchQuery query) {
        return luceneSearcher.search(query, getIndexSearcher(query));
    }
}
