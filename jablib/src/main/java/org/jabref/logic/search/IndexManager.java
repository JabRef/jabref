package org.jabref.logic.search;

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
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;

import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.indexing.BibFieldsIndexer;
import org.jabref.logic.search.indexing.DefaultLinkedFilesIndexer;
import org.jabref.logic.search.indexing.ReadOnlyLinkedFilesIndexer;
import org.jabref.logic.search.retrieval.BibFieldsSearcher;
import org.jabref.logic.search.retrieval.LinkedFilesSearcher;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
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
    private final BooleanProperty shouldIndexLinkedFiles;
    private final ChangeListener<Boolean> preferencesListener;
    private final BibFieldsIndexer bibFieldsIndexer;
    private final LuceneIndexer linkedFilesIndexer;
    private final BibFieldsSearcher bibFieldsSearcher;
    private final LinkedFilesSearcher linkedFilesSearcher;
    private final DelayTaskThrottler indexUpdateThrottler;
    private final ConcurrentHashMap<BibEntry, Set<Field>> pendingFieldsByEntry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BibEntry, FileDelta> pendingFileValuesByEntry = new ConcurrentHashMap<>();

    private record FileDelta(String oldValue, String newValue) {
    }

    public IndexManager(BibDatabaseContext databaseContext,
                        TaskExecutor executor,
                        CliPreferences preferences,
                        PostgreServer postgreServer) {
        this.taskExecutor = executor;
        this.databaseContext = databaseContext;
        this.shouldIndexLinkedFiles = preferences.getFilePreferences().fulltextIndexLinkedFilesProperty();
        this.preferencesListener = (_, _, newValue) -> bindToPreferences(newValue);
        this.shouldIndexLinkedFiles.addListener(preferencesListener);

        bibFieldsIndexer = new BibFieldsIndexer(preferences.getBibEntryPreferences(), databaseContext, postgreServer.getConnection());

        LuceneIndexer indexer;
        try {
            indexer = new DefaultLinkedFilesIndexer(databaseContext, preferences.getFilePreferences());
        } catch (IOException e) {
            LOGGER.debug("Error initializing linked files index - using read only index");
            indexer = new ReadOnlyLinkedFilesIndexer(databaseContext);
        }
        linkedFilesIndexer = indexer;

        this.bibFieldsSearcher = new BibFieldsSearcher(postgreServer.getConnection(), bibFieldsIndexer.getTable());
        this.linkedFilesSearcher = new LinkedFilesSearcher(databaseContext, linkedFilesIndexer, preferences.getFilePreferences());
        this.indexUpdateThrottler = taskExecutor.createThrottler(200);
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
            }.executeWith(taskExecutor);
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
        }.willBeRecoveredAutomatically(true)
         .onFinished(() -> this.databaseContext.getDatabase().postEvent(new IndexStartedEvent()))
         .executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
                    linkedFilesIndexer.updateOnStart(this);
                    return null;
                }
            }.executeWith(taskExecutor);
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
         .executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
                    linkedFilesIndexer.addToIndex(entries, this);
                    return null;
                }
            }.executeWith(taskExecutor);
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
         .executeWith(taskExecutor);

        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
                    linkedFilesIndexer.removeFromIndex(entries, this);
                    return null;
                }
            }.executeWith(taskExecutor);
        }
    }

    public void updateEntry(FieldChangedEvent event) {
        BibEntry entry = event.getBibEntry();
        Field field = event.getField();

        /// Accumulate which fields need updating for this entry
        pendingFieldsByEntry.computeIfAbsent(entry, _ -> ConcurrentHashMap.newKeySet()).add(field);

        /// FILE field updates rely on oldValue/newValue diffing in linkedFilesIndexer
        /// Intermediate events dropped by the throttler would corrupt the baseline,
        /// so we preserve the first oldValue seen and always update to the latest newValue
        if (field.equals(StandardField.FILE)) {
            pendingFileValuesByEntry.compute(entry, (_, existing) -> {
                if (existing == null) {
                    return new FileDelta(event.getOldValue(), event.getNewValue());
                } else {
                    return new FileDelta(existing.oldValue(), event.getNewValue());
                }
            });
        }

        indexUpdateThrottler.schedule(() -> {
            /// Snapshot and clear pending state atomically
            pendingFieldsByEntry.forEach((pendingEntry, fields) -> {
                if (pendingFieldsByEntry.remove(pendingEntry, fields)) {
                    Set<Field> fieldsSnapshot = Set.copyOf(fields);

                    new BackgroundTask<>() {
                        @Override
                        public Object call() {
                            for (Field snapshot : fieldsSnapshot) {
                                bibFieldsIndexer.updateEntry(pendingEntry, snapshot);
                            }
                            return null;
                        }
                    }.onFinished(() -> this.databaseContext.getDatabase()
                                                           .postEvent(new IndexAddedOrUpdatedEvent(List.of(pendingEntry))))
                     .executeWith(taskExecutor);

                    if (shouldIndexLinkedFiles.get() && fieldsSnapshot.contains(StandardField.FILE)) {
                        FileDelta fileValues = pendingFileValuesByEntry.remove(pendingEntry);
                        if (fileValues != null) {
                            new BackgroundTask<>() {
                                @Override
                                public Object call() {
                                    linkedFilesIndexer.updateEntry(pendingEntry, fileValues.oldValue(), fileValues.newValue(), this);
                                    return null;
                                }
                            }.executeWith(taskExecutor);
                        }
                    }
                }
            });
        });
    }

    public void rebuildFullTextIndex() {
        if (shouldIndexLinkedFiles.get()) {
            new BackgroundTask<>() {
                @Override
                public Object call() {
                    linkedFilesIndexer.rebuildIndex(this);
                    return null;
                }
            }.executeWith(taskExecutor);
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

    public SearchResults search(SearchQuery query) {
        List<Callable<SearchResults>> tasks = new ArrayList<>();
        tasks.add(() -> bibFieldsSearcher.search(query));

        if (query.getSearchFlags().contains(SearchFlags.FULLTEXT)) {
            tasks.add(() -> linkedFilesSearcher.search(query));
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
