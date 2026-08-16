package org.jabref.logic.search.sqlbased;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.search.sqlbased.indexing.DefaultLinkedFilesIndexer;
import org.jabref.logic.search.sqlbased.indexing.ReadOnlyLinkedFilesIndexer;
import org.jabref.logic.search.sqlbased.retrieval.LinkedFilesSearcher;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Maintains the Lucene index for linked files independently of the metadata search backend.
@NullMarked
public class LinkedFilesIndexManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkedFilesIndexManager.class);

    private final TaskExecutor taskExecutor;
    private final BooleanProperty shouldIndexLinkedFiles;
    private final ChangeListener<Boolean> preferencesListener;
    private final LuceneIndexer linkedFilesIndexer;
    private final LinkedFilesSearcher linkedFilesSearcher;
    private final DelayTaskThrottler indexUpdateThrottler;
    private final ConcurrentHashMap<String, FileDelta> pendingFileValuesByEntry = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private record FileDelta(String oldValue, String newValue) {
    }

    public LinkedFilesIndexManager(BibDatabaseContext databaseContext, TaskExecutor taskExecutor, FilePreferences filePreferences) {
        this.taskExecutor = taskExecutor;
        this.shouldIndexLinkedFiles = filePreferences.fulltextIndexLinkedFilesProperty();
        this.preferencesListener = (_, _, newValue) -> bindToPreferences(newValue);
        this.shouldIndexLinkedFiles.addListener(preferencesListener);

        LuceneIndexer indexer;
        try {
            indexer = new DefaultLinkedFilesIndexer(databaseContext, filePreferences);
        } catch (IOException e) {
            LOGGER.debug("Error initializing linked files index - using read only index");
            indexer = new ReadOnlyLinkedFilesIndexer(databaseContext);
        }
        linkedFilesIndexer = indexer;
        linkedFilesSearcher = new LinkedFilesSearcher(databaseContext, linkedFilesIndexer, filePreferences);
        indexUpdateThrottler = taskExecutor.createThrottler(700);
        updateOnStart();
    }

    private void bindToPreferences(boolean newValue) {
        if (newValue) {
            new BackgroundTask<>() {
                @Override
                public Void call() {
                    linkedFilesIndexer.updateOnStart(this);
                    return null;
                }
            }.executeWith(taskExecutor);
        } else {
            linkedFilesIndexer.removeAllFromIndex();
        }
    }

    private void updateOnStart() {
        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Void call() {
                    linkedFilesIndexer.updateOnStart(this);
                    return null;
                }
            }.executeWith(taskExecutor);
        }
    }

    public void addToIndex(List<BibEntry> entries) {
        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Void call() {
                    linkedFilesIndexer.addToIndex(entries, this);
                    return null;
                }
            }.executeWith(taskExecutor);
        }
    }

    public void removeFromIndex(List<BibEntry> entries) {
        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Void call() {
                    linkedFilesIndexer.removeFromIndex(entries, this);
                    return null;
                }
            }.executeWith(taskExecutor);
        }
    }

    public void updateEntry(FieldChangedEvent event) {
        if (closed.get() || !shouldIndexLinkedFiles.get() || !event.getField().equals(StandardField.FILE)) {
            return;
        }

        BibEntry entry = event.getBibEntry();
        String entryId = entry.getId();
        pendingFileValuesByEntry.compute(entryId, (_, existing) -> {
            if (existing == null) {
                return new FileDelta(event.getOldValue(), event.getNewValue());
            }
            return new FileDelta(existing.oldValue(), event.getNewValue());
        });

        indexUpdateThrottler.schedule(() -> {
            if (closed.get()) {
                return;
            }

            FileDelta fileValues = pendingFileValuesByEntry.remove(entryId);
            if (fileValues != null) {
                new BackgroundTask<>() {
                    @Override
                    public Void call() {
                        linkedFilesIndexer.updateEntry(entry, fileValues.oldValue(), fileValues.newValue(), this);
                        return null;
                    }
                }.executeWith(taskExecutor);
            }
        });
    }

    public void rebuildIndex() {
        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Void call() {
                    linkedFilesIndexer.rebuildIndex(this);
                    return null;
                }
            }.executeWith(taskExecutor);
        }
    }

    public SearchResults search(SearchQuery query) {
        return linkedFilesSearcher.search(query);
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        closeThrottler(false);
        shouldIndexLinkedFiles.removeListener(preferencesListener);
        linkedFilesIndexer.close();
    }

    public void closeAndWait() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        closeThrottler(true);
        shouldIndexLinkedFiles.removeListener(preferencesListener);
        linkedFilesIndexer.closeAndWait();
    }

    private void closeThrottler(boolean waitForShutdown) {
        indexUpdateThrottler.cancel();
        pendingFileValuesByEntry.clear();

        if (waitForShutdown) {
            indexUpdateThrottler.shutdown();
        } else {
            new BackgroundTask<>() {
                @Override
                public Void call() {
                    indexUpdateThrottler.shutdown();
                    return null;
                }
            }.executeWith(taskExecutor);
        }
    }
}
