package org.jabref.logic.search.sqlbased;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.sqlbased.indexing.BibFieldsIndexer;
import org.jabref.logic.search.sqlbased.retrieval.BibFieldsSearcher;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.event.IndexAddedOrUpdatedEvent;
import org.jabref.model.search.event.IndexClosedEvent;
import org.jabref.model.search.event.IndexRemovedEvent;
import org.jabref.model.search.event.IndexStartedEvent;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexManager.class);

    private final TaskExecutor taskExecutor;
    private final BibDatabaseContext databaseContext;
    private final BibFieldsIndexer bibFieldsIndexer;
    private final BibFieldsSearcher bibFieldsSearcher;
    private final LinkedFilesIndexManager linkedFilesIndexManager;
    private final DelayTaskThrottler indexUpdateThrottler;
    private final ConcurrentHashMap<String, PendingFieldUpdates> pendingFieldsByEntry = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private record PendingFieldUpdates(BibEntry entry, Set<Field> fields) {
    }

    public IndexManager(BibDatabaseContext databaseContext,
                        TaskExecutor executor,
                        CliPreferences preferences,
                        PostgresServer postgresServer) {
        this.taskExecutor = executor;
        this.databaseContext = databaseContext;

        bibFieldsIndexer = new BibFieldsIndexer(preferences.getBibEntryPreferences(), databaseContext, postgresServer.getConnection());
        this.bibFieldsSearcher = new BibFieldsSearcher(postgresServer.getConnection(), bibFieldsIndexer.getTable());
        this.linkedFilesIndexManager = new LinkedFilesIndexManager(databaseContext, taskExecutor, preferences.getFilePreferences());
        this.indexUpdateThrottler = taskExecutor.createThrottler(700);
        updateOnStart();
    }

    private void updateOnStart() {
        new BackgroundTask<>() {
            @Override
            public Void call() {
                bibFieldsIndexer.updateOnStart(this);
                return null;
            }
        }.willBeRecoveredAutomatically(true)
         .onFinished(() -> this.databaseContext.getDatabase().postEvent(new IndexStartedEvent()))
         .executeWith(taskExecutor);
    }

    public void addToIndex(List<BibEntry> entries) {
        new BackgroundTask<>() {
            @Override
            public Void call() {
                bibFieldsIndexer.addToIndex(entries, this);
                return null;
            }
        }.onFinished(() -> this.databaseContext.getDatabase().postEvent(new IndexAddedOrUpdatedEvent(entries)))
         .executeWith(taskExecutor);
        linkedFilesIndexManager.addToIndex(entries);
    }

    public void removeFromIndex(List<BibEntry> entries) {
        new BackgroundTask<>() {
            @Override
            public Void call() {
                bibFieldsIndexer.removeFromIndex(entries, this);
                return null;
            }
        }.onFinished(() -> this.databaseContext.getDatabase().postEvent(new IndexRemovedEvent(entries)))
         .executeWith(taskExecutor);
        linkedFilesIndexManager.removeFromIndex(entries);
    }

    public void updateEntry(FieldChangedEvent event) {
        if (closed.get()) {
            return;
        }

        BibEntry entry = event.getBibEntry();
        String entryId = entry.getId();
        Field field = event.getField();

        /// Accumulate which fields need updating for this entry
        /// Use `entryId` because hashCode of `entry` changes when fields are updated.
        /// Instead, `entryId` is stable.
        pendingFieldsByEntry.compute(entryId, (_, pendingUpdates) -> {
            if (pendingUpdates == null) {
                Set<Field> fields = ConcurrentHashMap.newKeySet();
                fields.add(field);
                return new PendingFieldUpdates(entry, fields);
            }

            pendingUpdates.fields().add(field);
            return new PendingFieldUpdates(entry, pendingUpdates.fields());
        });

        linkedFilesIndexManager.updateEntry(event);

        if (closed.get()) {
            pendingFieldsByEntry.remove(entryId);
            return;
        }

        indexUpdateThrottler.schedule(() -> {
            if (closed.get()) {
                return;
            }

            /// Snapshot and clear pending state atomically
            pendingFieldsByEntry.forEach((pendingEntryId, updates) -> {
                if (pendingFieldsByEntry.remove(pendingEntryId, updates)) {
                    BibEntry pendingEntry = updates.entry();
                    Set<Field> fieldsSnapshot = Set.copyOf(updates.fields());

                    new BackgroundTask<>() {
                        @Override
                        public Void call() {
                            for (Field snapshot : fieldsSnapshot) {
                                bibFieldsIndexer.updateEntry(pendingEntry, snapshot);
                            }
                            return null;
                        }
                    }.onFinished(() -> this.databaseContext.getDatabase()
                                                           .postEvent(new IndexAddedOrUpdatedEvent(List.of(pendingEntry))))
                     .executeWith(taskExecutor);
                }
            });
        });
    }

    public void rebuildFullTextIndex() {
        linkedFilesIndexManager.rebuildIndex();
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        closeThrottler(false);
        bibFieldsIndexer.close();
        linkedFilesIndexManager.close();
        databaseContext.getDatabase().postEvent(new IndexClosedEvent());
    }

    public void closeAndWait() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        closeThrottler(true);
        bibFieldsIndexer.closeAndWait();
        linkedFilesIndexManager.closeAndWait();
        databaseContext.getDatabase().postEvent(new IndexClosedEvent());
    }

    private void closeThrottler(boolean waitForShutdown) {
        indexUpdateThrottler.cancel();
        pendingFieldsByEntry.clear();

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

    public SearchResults search(SearchQuery query) {
        List<Callable<SearchResults>> tasks = new ArrayList<>();
        tasks.add(() -> bibFieldsSearcher.search(query));

        if (query.getSearchFlags().contains(SearchFlags.FULLTEXT)) {
            tasks.add(() -> linkedFilesIndexManager.search(query));
        }

        List<Future<SearchResults>> futures = HeadlessExecutorService.INSTANCE.executeAll(tasks);

        SearchResults searchResults = new SearchResults();
        for (Future<SearchResults> future : futures) {
            try {
                searchResults.mergeSearchResults(future.get());
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Error while searching", e);
            }
        }
        query.setSearchResults(searchResults);
        return searchResults;
    }

    /// @implNote No need to check for full-text searches as this method only used by the search groups
    public boolean isEntryMatched(BibEntry entry, SearchQuery query) {
        return bibFieldsSearcher.isMatched(entry, query);
    }

    public static void clearOldSearchIndices() {
        Path currentIndexPath = Directories.getFulltextIndexBaseDirectory();
        Path appData = currentIndexPath.getParent();

        try {
            Files.createDirectories(currentIndexPath);
        } catch (IOException e) {
            LOGGER.error("Could not create index directory {}", appData, e);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(appData)) {
            for (Path directory : stream) {
                if (Files.isDirectory(directory)
                        && !directory.toString().endsWith("ssl")
                        && directory.toString().contains("lucene")
                        && !directory.equals(currentIndexPath)) {
                    LOGGER.info("Deleting out-of-date fulltext search index at {}.", directory);

                    try (Stream<Path> indexPath = Files.walk(directory)) {
                        indexPath.sorted(Comparator.reverseOrder())
                                 .forEach(file -> {
                                     try {
                                         Files.deleteIfExists(file);
                                     } catch (IOException e) {
                                         LOGGER.error("Could not delete file {}", file, e);
                                     }
                                 });
                    } catch (IOException e) {
                        LOGGER.error("Could not read directory {}", directory, e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not access app-directory at {}", appData, e);
        }
    }
}
