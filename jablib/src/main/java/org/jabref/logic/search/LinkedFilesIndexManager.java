package org.jabref.logic.search;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.search.sqlbased.LuceneIndexer;
import org.jabref.logic.search.sqlbased.indexing.DefaultLinkedFilesIndexer;
import org.jabref.logic.search.sqlbased.indexing.ReadOnlyLinkedFilesIndexer;
import org.jabref.logic.search.sqlbased.retrieval.LinkedFilesSearcher;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.SerialBackgroundTaskGroup;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
    private final SerialBackgroundTaskGroup indexingTasks;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private record FileDelta(String oldValue, String newValue) {
    }

    public LinkedFilesIndexManager(BibDatabaseContext databaseContext, TaskExecutor taskExecutor, FilePreferences filePreferences) {
        this(databaseContext, taskExecutor, filePreferences, createIndexer(databaseContext, filePreferences));
    }

    LinkedFilesIndexManager(BibDatabaseContext databaseContext, TaskExecutor taskExecutor, FilePreferences filePreferences, LuceneIndexer indexer) {
        this.taskExecutor = taskExecutor;
        this.indexingTasks = new SerialBackgroundTaskGroup(taskExecutor);
        this.shouldIndexLinkedFiles = filePreferences.fulltextIndexLinkedFilesProperty();
        this.preferencesListener = (_, _, newValue) -> bindToPreferences(newValue);
        this.shouldIndexLinkedFiles.addListener(preferencesListener);
        linkedFilesIndexer = indexer;
        linkedFilesSearcher = new LinkedFilesSearcher(databaseContext, linkedFilesIndexer, filePreferences);
        indexUpdateThrottler = taskExecutor.createThrottler(700);
        updateOnStart();
    }

    private static LuceneIndexer createIndexer(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        try {
            return new DefaultLinkedFilesIndexer(databaseContext, filePreferences);
        } catch (IOException e) {
            LOGGER.debug("Error initializing linked files index - using read only index", e);
            return new ReadOnlyLinkedFilesIndexer(databaseContext);
        }
    }

    // [impl->req~search.fulltext.disable-indexing~1]
    private void bindToPreferences(boolean newValue) {
        if (newValue) {
            indexingTasks.cancel();
            runIndexingTask(task -> {
                linkedFilesIndexer.removeAllFromIndex();
                linkedFilesIndexer.updateOnStart(task);
            });
        } else {
            indexUpdateThrottler.cancel();
            pendingFileValuesByEntry.clear();
            indexingTasks.cancelAndRunWhenIdle(linkedFilesIndexer::removeAllFromIndex);
        }
    }

    private void runIndexingTask(Consumer<BackgroundTask<?>> work) {
        if (!closed.get() && shouldIndexLinkedFiles.get()) {
            indexingTasks.submit(work);
        }
    }

    private void updateOnStart() {
        if (shouldIndexLinkedFiles.get()) {
            runIndexingTask(linkedFilesIndexer::updateOnStart);
        }
    }

    public void addToIndex(List<BibEntry> entries) {
        if (shouldIndexLinkedFiles.get()) {
            runIndexingTask(task -> linkedFilesIndexer.addToIndex(entries, task));
        }
    }

    public void removeFromIndex(List<BibEntry> entries) {
        if (shouldIndexLinkedFiles.get()) {
            runIndexingTask(task -> linkedFilesIndexer.removeFromIndex(entries, task));
        }
    }

    public void updateEntry(FieldChangedEvent event) {
        if (closed.get() || !shouldIndexLinkedFiles.get() || !event.getField().equals(StandardField.FILE)) {
            return;
        }

        BibEntry entry = event.getBibEntry();
        String entryId = entry.getId();
        pendingFileValuesByEntry.compute(entryId, (_, existing) -> {
            return Optional.ofNullable(existing)
                           .map(fileDelta -> new FileDelta(fileDelta.oldValue(), normalizeNullToEmpty(event.getNewValue())))
                           .orElseGet(() -> new FileDelta(normalizeNullToEmpty(event.getOldValue()), normalizeNullToEmpty(event.getNewValue())));
        });

        indexUpdateThrottler.schedule(() -> {
            if (closed.get() || !shouldIndexLinkedFiles.get()) {
                return;
            }

            Optional.ofNullable(pendingFileValuesByEntry.remove(entryId)).ifPresent(fileValues ->
                    runIndexingTask(task -> linkedFilesIndexer.updateEntry(entry, fileValues.oldValue(), fileValues.newValue(), task)));
        });
    }

    private static String normalizeNullToEmpty(@Nullable String value) {
        return Optional.ofNullable(value).orElse("");
    }

    public void rebuildIndex() {
        if (shouldIndexLinkedFiles.get()) {
            runIndexingTask(linkedFilesIndexer::rebuildIndex);
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
        indexingTasks.close(linkedFilesIndexer::closeAndWait);
    }

    public void closeAndWait() {
        if (!closed.compareAndSet(false, true)) {
            indexingTasks.closeAndWait(linkedFilesIndexer::closeAndWait);
            return;
        }
        closeThrottler(true);
        shouldIndexLinkedFiles.removeListener(preferencesListener);
        indexingTasks.closeAndWait(linkedFilesIndexer::closeAndWait);
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
