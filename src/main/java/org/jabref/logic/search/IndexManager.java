package org.jabref.logic.search;

import java.io.IOException;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.search.indexing.DefaultLinkedFilesIndexer;
import org.jabref.logic.search.indexing.PostgreIndexer;
import org.jabref.logic.search.indexing.ReadOnlyLinkedFilesIndexer;
import org.jabref.logic.search.retrieval.LinkedFilesSearcher;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;
import org.jabref.model.search.event.IndexAddedOrUpdatedEvent;
import org.jabref.model.search.event.IndexClosedEvent;
import org.jabref.model.search.event.IndexRemovedEvent;
import org.jabref.model.search.event.IndexStartedEvent;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexManager.class);

    private final TaskExecutor taskExecutor;
    private final BibDatabaseContext databaseContext;
    private final BooleanProperty shouldIndexLinkedFiles;
    private final BooleanProperty isLinkedFilesIndexerBlocked = new SimpleBooleanProperty(false);
    private final ChangeListener<Boolean> preferencesListener;
    private final PostgreIndexer bibFieldsIndexer;
    private final LuceneIndexer linkedFilesIndexer;
    private final LinkedFilesSearcher linkedFilesSearcher;

    public IndexManager(BibDatabaseContext databaseContext, TaskExecutor executor, FilePreferences preferences) {
        this.taskExecutor = executor;
        this.databaseContext = databaseContext;
        this.shouldIndexLinkedFiles = preferences.fulltextIndexLinkedFilesProperty();
        this.preferencesListener = (observable, oldValue, newValue) -> bindToPreferences(newValue);
        this.shouldIndexLinkedFiles.addListener(preferencesListener);

        PostgreServer postgreServer = Injector.instantiateModelOrService(PostgreServer.class);
        bibFieldsIndexer = new PostgreIndexer(databaseContext, postgreServer.getConnection());

        LuceneIndexer indexer;
        try {
            indexer = new DefaultLinkedFilesIndexer(databaseContext, preferences);
        } catch (IOException e) {
            LOGGER.debug("Error initializing linked files index - using read only index");
            indexer = new ReadOnlyLinkedFilesIndexer(databaseContext);
        }
        linkedFilesIndexer = indexer;

        this.linkedFilesSearcher = new LinkedFilesSearcher(databaseContext, linkedFilesIndexer, preferences);
        updateOnStart();
    }

    private void bindToPreferences(boolean newValue) {
        if (newValue) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
                    linkedFilesIndexer.updateOnStart(this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        } else {
            linkedFilesIndexer.removeAllFromIndex();
        }
    }

    private void updateOnStart() {
        new BackgroundTask<>() {
            @Override
            public Object call() {
                bibFieldsIndexer.updateOnStart(this);
                return null;
            }
        }.showToUser(true)
         .willBeRecoveredAutomatically(true)
         .onFinished(() -> this.databaseContext.getDatabase().postEvent(new IndexStartedEvent()))
         .executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
                    linkedFilesIndexer.updateOnStart(this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        }
    }

    public void addToIndex(List<BibEntry> entries) {
        new BackgroundTask<>() {
            @Override
            public Object call() {
                bibFieldsIndexer.addToIndex(entries, this);
                return null;
            }
        }.onFinished(() -> this.databaseContext.getDatabase().postEvent(new IndexAddedOrUpdatedEvent(entries)))
         .showToUser(true).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
                    linkedFilesIndexer.addToIndex(entries, this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        }
    }

    public void removeFromIndex(List<BibEntry> entries) {
        new BackgroundTask<>() {
            @Override
            public Object call() {
                bibFieldsIndexer.removeFromIndex(entries, this);
                return null;
            }
        }.onFinished(() -> this.databaseContext.getDatabase().postEvent(new IndexRemovedEvent(entries)))
         .showToUser(true).executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
                    linkedFilesIndexer.removeFromIndex(entries, this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        }
    }

    public void updateEntry(FieldChangedEvent event) {
        new BackgroundTask<>() {
            @Override
            public Object call() {
                bibFieldsIndexer.updateEntry(event.getBibEntry(), event.getField());
                return null;
            }
        }.onFinished(() -> this.databaseContext.getDatabase().postEvent(new IndexAddedOrUpdatedEvent(List.of(event.getBibEntry()))))
         .executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get() && event.getField().equals(StandardField.FILE) && !isLinkedFilesIndexerBlocked.get()) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
                    linkedFilesIndexer.updateEntry(event.getBibEntry(), event.getOldValue(), event.getNewValue(), this);
                    return null;
                }
            }.executeWith(taskExecutor);
        }
    }

    public void updateAfterDropFiles(BibEntry entry) {
        new BackgroundTask<>() {
            @Override
            public Object call() {
                bibFieldsIndexer.updateEntry(entry, StandardField.FILE);
                return null;
            }
        }.onFinished(() -> this.databaseContext.getDatabase().postEvent(new IndexAddedOrUpdatedEvent(List.of(entry))))
         .executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get() && !isLinkedFilesIndexerBlocked.get()) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
                    linkedFilesIndexer.addToIndex(List.of(entry), this);
                    return null;
                }
            }.showToUser(true).executeWith(taskExecutor);
        }
    }

    public void rebuildFullTextIndex() {
        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
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
        databaseContext.getDatabase().postEvent(new IndexClosedEvent());
    }

    public void closeAndWait() {
        bibFieldsIndexer.closeAndWait();
        shouldIndexLinkedFiles.removeListener(preferencesListener);
        linkedFilesIndexer.closeAndWait();
        databaseContext.getDatabase().postEvent(new IndexClosedEvent());
    }

    public AutoCloseable blockLinkedFileIndexer() {
        LOGGER.debug("Blocking linked files indexer");
        isLinkedFilesIndexerBlocked.set(true);
        return () -> isLinkedFilesIndexerBlocked.set(false);
    }

    public SearchResults search(SearchQuery query) {
        if (query.isValid()) {
            query.setSearchResults(linkedFilesSearcher.search(query.getParsedQuery(), query.getSearchFlags()));
        } else {
            query.setSearchResults(new SearchResults());
        }
        return query.getSearchResults();
    }

    public boolean isEntryMatched(BibEntry entry, SearchQuery query) {
        return true;
    }
}
