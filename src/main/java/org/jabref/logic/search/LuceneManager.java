package org.jabref.logic.search;

import java.io.IOException;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.search.indexing.BibFieldsIndexer;
import org.jabref.logic.search.indexing.DefaultLinkedFilesIndexer;
import org.jabref.logic.search.indexing.ReadOnlyLinkedFilesIndexer;
import org.jabref.logic.search.retrieval.LuceneSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.LuceneIndexer;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.SearchQuery;
import org.jabref.model.search.SearchResults;
import org.jabref.model.search.envent.IndexAddedOrUpdatedEvent;
import org.jabref.model.search.envent.IndexRemovedEvent;
import org.jabref.model.search.envent.IndexStartedEvent;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.EventBus;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuceneManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneManager.class);
    private final EventBus eventBus = new EventBus();
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
        this.isLinkedFilesIndexerBlocked = new SimpleBooleanProperty(false);
        this.shouldIndexLinkedFiles = preferences.getFilePreferences().fulltextIndexLinkedFilesProperty();
        this.preferencesListener = (observable, oldValue, newValue) -> bindToPreferences(newValue);
        this.shouldIndexLinkedFiles.addListener(preferencesListener);
        this.luceneSearcher = new LuceneSearcher(databaseContext);

        initializeIndexers();
    }

    private void initializeIndexers() {
        try {
            bibFieldsIndexer = new BibFieldsIndexer(databaseContext);
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
            linkedFilesIndexer = new DefaultLinkedFilesIndexer(databaseContext, preferences.getFilePreferences());
        } catch (IOException e) {
            LOGGER.debug("Error initializing linked files index - using read only index");
            linkedFilesIndexer = new ReadOnlyLinkedFilesIndexer(databaseContext);
        }
    }

    private void bindToPreferences(boolean newValue) {
        if (newValue) {
            initializeLinkedFilesIndexer();
            new BackgroundTask<>() {
                @Override
                protected Object call() {
                    linkedFilesIndexer.updateOnStart(this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        } else {
            linkedFilesIndexer.removeAllFromIndex();
            linkedFilesIndexer.close();
            linkedFilesIndexer = null;
        }
    }

    public void updateOnStart() {
        new BackgroundTask<>() {
            @Override
            protected Object call() {
                bibFieldsIndexer.updateOnStart(this);
                return null;
            }
        }.showToUser(true)
         .onFinished(() -> this.eventBus.post(new IndexStartedEvent()))
         .executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get() && linkedFilesIndexer != null) {
            new BackgroundTask<>() {
                @Override
                protected Object call() {
                    linkedFilesIndexer.updateOnStart(this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        }
    }

    public void addToIndex(List<BibEntry> entries) {
        new BackgroundTask<>() {
            @Override
            protected Object call() {
                bibFieldsIndexer.addToIndex(entries, this);
                return null;
            }
        }.onFinished(() -> this.eventBus.post(new IndexAddedOrUpdatedEvent(entries)))
         .showToUser(true).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get() && linkedFilesIndexer != null) {
            new BackgroundTask<>() {
                @Override
                protected Object call() {
                    linkedFilesIndexer.addToIndex(entries, this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        }
    }

    public void removeFromIndex(List<BibEntry> entries) {
        new BackgroundTask<>() {
            @Override
            protected Object call() {
                bibFieldsIndexer.removeFromIndex(entries, this);
                return null;
            }
        }.onFinished(() -> this.eventBus.post(new IndexRemovedEvent()))
        .showToUser(true).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get() && linkedFilesIndexer != null) {
            new BackgroundTask<>() {
                @Override
                protected Object call() {
                    linkedFilesIndexer.removeFromIndex(entries, this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        }
    }

    public void updateEntry(BibEntry entry, String oldValue, String newValue, boolean isLinkedFile) {
        new BackgroundTask<>() {
            @Override
            protected Object call() {
                bibFieldsIndexer.updateEntry(entry, oldValue, newValue, this);
                return null;
            }
        }.onFinished(() -> this.eventBus.post(new IndexAddedOrUpdatedEvent(List.of(entry))))
         .showToUser(true).executeWith(taskExecutor);

        if (isLinkedFile && shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get() && linkedFilesIndexer != null) {
            new BackgroundTask<>() {
                @Override
                protected Object call() {
                    linkedFilesIndexer.updateEntry(entry, oldValue, newValue, this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        }
    }

    public void updateAfterDropFiles(BibEntry entry) {
        new BackgroundTask<>() {
            @Override
            protected Object call() {
                bibFieldsIndexer.updateEntry(entry, "", "", this);
                return null;
            }
        }.onFinished(() -> this.eventBus.post(new IndexAddedOrUpdatedEvent(List.of(entry))))
        .showToUser(true).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get() && linkedFilesIndexer != null) {
            new BackgroundTask<>() {
                @Override
                protected Object call() {
                    linkedFilesIndexer.addToIndex(List.of(entry), this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        }
    }

    public void rebuildIndex() {
        new BackgroundTask<>() {
            @Override
            protected Object call() {
                bibFieldsIndexer.rebuildIndex(this);
                return null;
            }
        }.onFinished(() -> this.eventBus.post(new IndexStartedEvent()))
         .showToUser(true).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get() && linkedFilesIndexer != null) {
            new BackgroundTask<>() {
                @Override
                protected Object call() {
                    linkedFilesIndexer.rebuildIndex(this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
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
        LOGGER.debug("Blocking linked files indexer");
        isLinkedFilesIndexerBlocked.set(true);
        return () -> isLinkedFilesIndexerBlocked.set(false);
    }

    public IndexSearcher getIndexSearcher(SearchQuery query) {
        if (query.getSearchFlags().contains(SearchFlags.FULLTEXT) && shouldIndexLinkedFiles.get()) {
            try {
                MultiReader reader = new MultiReader(bibFieldsIndexer.getIndexSearcher().getIndexReader(), linkedFilesIndexer.getIndexSearcher().getIndexReader());
                LOGGER.debug("Using index searcher for bib fields and linked files");
                return new IndexSearcher(reader);
            } catch (IOException e) {
                LOGGER.error("Error getting index searcher", e);
            }
        }
        LOGGER.debug("Using index searcher for bib fields only");
        return bibFieldsIndexer.getIndexSearcher();
    }

    public SearchResults search(SearchQuery query) {
        return luceneSearcher.search(query, getIndexSearcher(query));
    }

    public void registerListener(Object listener) {
        this.eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        eventBus.unregister(listener);
    }
}
