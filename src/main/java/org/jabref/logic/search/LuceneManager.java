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
import org.jabref.model.search.SearchQuery;
import org.jabref.model.search.SearchResults;
import org.jabref.model.search.envent.IndexAddedOrUpdatedEvent;
import org.jabref.model.search.envent.IndexRemovedEvent;
import org.jabref.model.search.envent.IndexStartedEvent;
import org.jabref.preferences.FilePreferences;

import com.google.common.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuceneManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuceneManager.class);

    private final EventBus eventBus = new EventBus();
    private final TaskExecutor taskExecutor;
    private final BooleanProperty shouldIndexLinkedFiles;
    private final BooleanProperty isLinkedFilesIndexerBlocked = new SimpleBooleanProperty(false);;
    private final ChangeListener<Boolean> preferencesListener;
    private final LuceneSearcher luceneSearcher;
    private final LuceneIndexer bibFieldsIndexer;
    private final LuceneIndexer linkedFilesIndexer;

    public LuceneManager(BibDatabaseContext databaseContext, TaskExecutor executor, FilePreferences preferences) {
        this.taskExecutor = executor;

        this.shouldIndexLinkedFiles = preferences.fulltextIndexLinkedFilesProperty();
        this.preferencesListener = (observable, oldValue, newValue) -> bindToPreferences(newValue);
        this.shouldIndexLinkedFiles.addListener(preferencesListener);

        this.bibFieldsIndexer = new BibFieldsIndexer(databaseContext);

        LuceneIndexer indexer;
        try {
            indexer = new DefaultLinkedFilesIndexer(databaseContext, preferences);
        } catch (IOException e) {
            LOGGER.debug("Error initializing linked files index - using read only index");
            indexer = new ReadOnlyLinkedFilesIndexer(databaseContext);
        }
        linkedFilesIndexer = indexer;

        this.luceneSearcher = new LuceneSearcher(databaseContext, bibFieldsIndexer, linkedFilesIndexer);
    }

    private void bindToPreferences(boolean newValue) {
        if (newValue) {
            new BackgroundTask<>() {
                @Override
                protected Object call() {
                    linkedFilesIndexer.updateOnStart(this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        } else {
            linkedFilesIndexer.removeAllFromIndex();
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

        if (shouldIndexLinkedFiles.get()) {
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

        if (shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
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

        if (shouldIndexLinkedFiles.get()) {
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

        if (isLinkedFile && shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
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

        if (shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
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

        if (shouldIndexLinkedFiles.get()) {
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
        linkedFilesIndexer.close();
    }

    public AutoCloseable blockLinkedFileIndexer() {
        LOGGER.debug("Blocking linked files indexer");
        isLinkedFilesIndexerBlocked.set(true);
        return () -> isLinkedFilesIndexerBlocked.set(false);
    }

    public void registerListener(Object listener) {
        this.eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        eventBus.unregister(listener);
    }

    public SearchResults search(SearchQuery query) {
        if (query.isValid()) {
            return luceneSearcher.search(query.getParsedQuery(), query.getSearchFlags());
        }
        return new SearchResults();
    }

    public boolean isMatched(BibEntry entry, SearchQuery query) {
        return luceneSearcher.isMatched(entry, query);
    }
}
