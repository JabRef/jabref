package org.jabref.logic.search;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.search.sqlbased.LuceneIndexer;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.LinkedFilesConstants;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class LinkedFilesIndexManagerTest {
    @Test
        // [utest->req~search.fulltext.disable-indexing~1]
    void disablingAndReenablingWaitsForCancelledWorker() throws Exception {
        try (ExecutorService workers = Executors.newCachedThreadPool()) {
            TaskExecutor executor = mock(TaskExecutor.class);
            when(executor.createThrottler(anyInt())).thenReturn(new DelayTaskThrottler(700));
            when(executor.execute(any())).thenAnswer(invocation -> {
                BackgroundTask<?> task = invocation.getArgument(0);
                return workers.submit(task::call);
            });
            SimpleBooleanProperty enabled = new SimpleBooleanProperty(false);
            FilePreferences preferences = mock(FilePreferences.class);
            when(preferences.fulltextIndexLinkedFilesProperty()).thenReturn(enabled);
            LuceneIndexer indexer = mock(LuceneIndexer.class);
            CountDownLatch firstStarted = new CountDownLatch(1);
            CountDownLatch releaseFirst = new CountDownLatch(1);
            CountDownLatch secondStarted = new CountDownLatch(1);
            AtomicInteger starts = new AtomicInteger();
            AtomicReference<@Nullable BackgroundTask<?>> firstTask = new AtomicReference<>();
            doAnswer(invocation -> {
                if (starts.incrementAndGet() == 1) {
                    firstTask.set(invocation.getArgument(0));
                    firstStarted.countDown();
                    releaseFirst.await();
                } else {
                    secondStarted.countDown();
                }
                return null;
            }).when(indexer).updateOnStart(any());
            LinkedFilesIndexManager manager = new LinkedFilesIndexManager(new BibDatabaseContext(), executor, preferences, indexer);
            try {
                enabled.set(true);
                assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
                enabled.set(false);
                enabled.set(true);
                var first = firstTask.get();
                assertNotNull(first);
                assertTrue(first.isCancelled());
                assertFalse(secondStarted.await(100, TimeUnit.MILLISECONDS));
                releaseFirst.countDown();
                assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
                verify(indexer, times(2)).removeAllFromIndex();
            } finally {
                releaseFirst.countDown();
                manager.closeAndWait();
            }
        }
    }

    @Test
        // [utest->req~search.fulltext.disable-indexing~1]
    void disablingWaitsForRunningWorkBeforeClearingIndex() throws Exception {
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            TaskExecutor executor = mock(TaskExecutor.class);
            when(executor.createThrottler(anyInt())).thenReturn(new DelayTaskThrottler(700));
            when(executor.execute(any())).thenAnswer(invocation -> {
                BackgroundTask<?> task = invocation.getArgument(0);
                return workers.submit(task::call);
            });
            SimpleBooleanProperty enabled = new SimpleBooleanProperty(false);
            FilePreferences preferences = mock(FilePreferences.class);
            when(preferences.fulltextIndexLinkedFilesProperty()).thenReturn(enabled);
            LuceneIndexer indexer = mock(LuceneIndexer.class);
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch cleared = new CountDownLatch(1);
            CompletableFuture<Void> release = new CompletableFuture<>();
            AtomicInteger clears = new AtomicInteger();
            doAnswer(_ -> {
                if (clears.incrementAndGet() == 2) {
                    cleared.countDown();
                }
                return null;
            }).when(indexer).removeAllFromIndex();
            doAnswer(_ -> {
                started.countDown();
                release.join();
                return null;
            }).when(indexer).updateOnStart(any());
            LinkedFilesIndexManager manager = new LinkedFilesIndexManager(new BibDatabaseContext(), executor, preferences, indexer);
            try {
                enabled.set(true);
                assertTrue(started.await(5, TimeUnit.SECONDS));
                enabled.set(false);
                assertEquals(1, clears.get());
                release.complete(null);
                assertTrue(cleared.await(5, TimeUnit.SECONDS));
                assertEquals(2, clears.get());
            } finally {
                release.complete(null);
                manager.closeAndWait();
            }
        }
    }

    @Test
    void closeAndWaitWaitsForRunningWorker() throws Exception {
        try (ExecutorService workers = Executors.newCachedThreadPool()) {
            TaskExecutor executor = mock(TaskExecutor.class);
            when(executor.createThrottler(anyInt())).thenReturn(new DelayTaskThrottler(700));
            when(executor.execute(any())).thenAnswer(invocation -> {
                BackgroundTask<?> task = invocation.getArgument(0);
                return workers.submit(task::call);
            });
            SimpleBooleanProperty enabled = new SimpleBooleanProperty(false);
            FilePreferences preferences = mock(FilePreferences.class);
            when(preferences.fulltextIndexLinkedFilesProperty()).thenReturn(enabled);
            LuceneIndexer indexer = mock(LuceneIndexer.class);
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            CountDownLatch cancelled = new CountDownLatch(1);
            doAnswer(invocation -> {
                BackgroundTask<?> task = invocation.getArgument(0);
                task.isCancelledProperty().addListener((_, _, value) -> {
                    if (value) {
                        cancelled.countDown();
                    }
                });
                started.countDown();
                release.await();
                return null;
            }).when(indexer).updateOnStart(any());
            LinkedFilesIndexManager manager = new LinkedFilesIndexManager(new BibDatabaseContext(), executor, preferences, indexer);
            try {
                enabled.set(true);
                assertTrue(started.await(5, TimeUnit.SECONDS));
                Future<?> closing = workers.submit(manager::closeAndWait);
                assertTrue(cancelled.await(5, TimeUnit.SECONDS));
                assertFalse(closing.isDone());
                verify(indexer, never()).closeAndWait();
                release.countDown();
                closing.get(5, TimeUnit.SECONDS);
                verify(indexer).closeAndWait();
            } finally {
                release.countDown();
                manager.closeAndWait();
            }
        }
    }

    @Test
        // [utest->req~search.fulltext.disable-indexing~1]
    void queuedWorkFromPreviousGenerationDoesNotRun() throws Exception {
        TaskExecutor executor = mock(TaskExecutor.class);
        when(executor.createThrottler(anyInt())).thenReturn(new DelayTaskThrottler(700));
        List<BackgroundTask<?>> tasks = new ArrayList<>();
        when(executor.execute(any())).thenAnswer(invocation -> {
            tasks.add(invocation.getArgument(0));
            return CompletableFuture.completedFuture(null);
        });
        SimpleBooleanProperty enabled = new SimpleBooleanProperty(false);
        FilePreferences preferences = mock(FilePreferences.class);
        when(preferences.fulltextIndexLinkedFilesProperty()).thenReturn(enabled);
        LuceneIndexer indexer = mock(LuceneIndexer.class);
        LinkedFilesIndexManager manager = new LinkedFilesIndexManager(new BibDatabaseContext(), executor, preferences, indexer);
        try {
            enabled.set(true);
            enabled.set(false);
            enabled.set(true);
            tasks.getFirst().call();
            verify(indexer, never()).updateOnStart(any());
            tasks.getLast().call();
            verify(indexer).updateOnStart(any());
        } finally {
            manager.closeAndWait();
        }
    }

    @Test
    void workCheckedBeforeDisablingIsNotSubmittedAfterDisabling() throws Exception {
        TaskExecutor executor = mock(TaskExecutor.class);
        when(executor.createThrottler(anyInt())).thenReturn(new DelayTaskThrottler(700));
        List<BackgroundTask<?>> tasks = new CopyOnWriteArrayList<>();
        when(executor.execute(any())).thenAnswer(invocation -> {
            tasks.add(invocation.getArgument(0));
            return CompletableFuture.completedFuture(null);
        });
        PausedPreference enabled = new PausedPreference();
        FilePreferences preferences = mock(FilePreferences.class);
        when(preferences.fulltextIndexLinkedFilesProperty()).thenReturn(enabled);
        LuceneIndexer indexer = mock(LuceneIndexer.class);
        LinkedFilesIndexManager manager = new LinkedFilesIndexManager(new BibDatabaseContext(), executor, preferences, indexer);
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                enabled.set(true);
                tasks.getFirst().call();
                Future<?> submission = workers.submit(() -> {
                    enabled.reader.set(Thread.currentThread());
                    manager.addToIndex(List.of(new BibEntry()));
                });
                assertTrue(enabled.readPaused.await(5, TimeUnit.SECONDS));
                enabled.set(false);
                enabled.resume.complete(null);
                submission.get(5, TimeUnit.SECONDS);
                if (tasks.size() > 1) {
                    tasks.getLast().call();
                }
                verify(indexer, never()).addToIndex(any(), any());
            } finally {
                enabled.resume.complete(null);
                manager.closeAndWait();
            }
        }
    }

    @Test
    void workRequestedBeforeDisablingIsNotResubmittedAfterReenabling() throws Exception {
        TaskExecutor executor = mock(TaskExecutor.class);
        when(executor.createThrottler(anyInt())).thenReturn(new DelayTaskThrottler(700));
        List<BackgroundTask<?>> tasks = new CopyOnWriteArrayList<>();
        when(executor.execute(any())).thenAnswer(invocation -> {
            tasks.add(invocation.getArgument(0));
            return CompletableFuture.completedFuture(null);
        });
        PausedPreference enabled = new PausedPreference(1);
        FilePreferences preferences = mock(FilePreferences.class);
        when(preferences.fulltextIndexLinkedFilesProperty()).thenReturn(enabled);
        LuceneIndexer indexer = mock(LuceneIndexer.class);
        LinkedFilesIndexManager manager = new LinkedFilesIndexManager(new BibDatabaseContext(), executor, preferences, indexer);
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                enabled.set(true);
                tasks.getFirst().call();
                Future<?> submission = workers.submit(() -> {
                    enabled.reader.set(Thread.currentThread());
                    manager.addToIndex(List.of(new BibEntry()));
                });
                assertTrue(enabled.readPaused.await(5, TimeUnit.SECONDS));
                enabled.set(false);
                enabled.set(true);
                enabled.resume.complete(null);
                submission.get(5, TimeUnit.SECONDS);
                tasks.get(1).call();
                if (tasks.size() > 2) {
                    tasks.getLast().call();
                }
                verify(indexer, never()).addToIndex(any(), any());
            } finally {
                enabled.resume.complete(null);
                manager.closeAndWait();
            }
        }
    }

    @Test
    void staleThrottledUpdateDoesNotDiscardNewGenerationFileChange() throws Exception {
        TaskExecutor executor = mock(TaskExecutor.class);
        DelayTaskThrottler throttler = mock(DelayTaskThrottler.class);
        when(executor.createThrottler(anyInt())).thenReturn(throttler);
        List<Runnable> callbacks = new CopyOnWriteArrayList<>();
        when(throttler.schedule(any())).thenAnswer(invocation -> {
            callbacks.add(invocation.getArgument(0));
            return mock(ScheduledFuture.class);
        });
        List<BackgroundTask<?>> tasks = new CopyOnWriteArrayList<>();
        when(executor.execute(any())).thenAnswer(invocation -> {
            tasks.add(invocation.getArgument(0));
            return CompletableFuture.completedFuture(null);
        });
        PausedPreference enabled = new PausedPreference(1);
        FilePreferences preferences = mock(FilePreferences.class);
        when(preferences.fulltextIndexLinkedFilesProperty()).thenReturn(enabled);
        LuceneIndexer indexer = mock(LuceneIndexer.class);
        LinkedFilesIndexManager manager = new LinkedFilesIndexManager(new BibDatabaseContext(), executor, preferences, indexer);
        BibEntry entry = new BibEntry();
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                enabled.set(true);
                tasks.getFirst().call();
                manager.updateEntry(new FieldChangedEvent(entry, StandardField.FILE, "old", "first"));
                Future<?> oldCallback = workers.submit(() -> {
                    enabled.reader.set(Thread.currentThread());
                    callbacks.getFirst().run();
                });
                assertTrue(enabled.readPaused.await(5, TimeUnit.SECONDS));
                enabled.set(false);
                enabled.set(true);
                manager.updateEntry(new FieldChangedEvent(entry, StandardField.FILE, "first", "second"));
                enabled.resume.complete(null);
                oldCallback.get(5, TimeUnit.SECONDS);
                tasks.get(1).call();
                callbacks.getLast().run();
                if (tasks.size() > 2) {
                    tasks.getLast().call();
                }
                verify(indexer).updateEntry(eq(entry), eq("first"), eq("second"), any());
            } finally {
                enabled.resume.complete(null);
                manager.closeAndWait();
            }
        }
    }

    @Test
    void staleFileChangeDoesNotReplaceNewGenerationScheduledUpdate() throws Exception {
        TaskExecutor executor = mock(TaskExecutor.class);
        DelayTaskThrottler throttler = mock(DelayTaskThrottler.class);
        when(executor.createThrottler(anyInt())).thenReturn(throttler);
        AtomicReference<@Nullable Runnable> scheduled = new AtomicReference<>();
        when(throttler.schedule(any())).thenAnswer(invocation -> {
            scheduled.set(invocation.getArgument(0));
            return mock(ScheduledFuture.class);
        });
        List<BackgroundTask<?>> tasks = new CopyOnWriteArrayList<>();
        when(executor.execute(any())).thenAnswer(invocation -> {
            tasks.add(invocation.getArgument(0));
            return CompletableFuture.completedFuture(null);
        });
        PausedPreference enabled = new PausedPreference(1);
        FilePreferences preferences = mock(FilePreferences.class);
        when(preferences.fulltextIndexLinkedFilesProperty()).thenReturn(enabled);
        LuceneIndexer indexer = mock(LuceneIndexer.class);
        LinkedFilesIndexManager manager = new LinkedFilesIndexManager(new BibDatabaseContext(), executor, preferences, indexer);
        BibEntry entry = new BibEntry();
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            try {
                enabled.set(true);
                tasks.getFirst().call();
                Future<?> oldChange = workers.submit(() -> {
                    enabled.reader.set(Thread.currentThread());
                    manager.updateEntry(new FieldChangedEvent(entry, StandardField.FILE, "old", "first"));
                });
                assertTrue(enabled.readPaused.await(5, TimeUnit.SECONDS));
                enabled.set(false);
                enabled.set(true);
                manager.updateEntry(new FieldChangedEvent(entry, StandardField.FILE, "first", "second"));
                enabled.resume.complete(null);
                oldChange.get(5, TimeUnit.SECONDS);
                tasks.get(1).call();
                var callback = scheduled.get();
                assertNotNull(callback);
                callback.run();
                if (tasks.size() > 2) {
                    tasks.getLast().call();
                }
                verify(indexer).updateEntry(eq(entry), eq("first"), eq("second"), any());
            } finally {
                enabled.resume.complete(null);
                manager.closeAndWait();
            }
        }
    }

    @Test
    void closingAfterDisablingClearsIndexBeforeClosing() throws Exception {
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            TaskExecutor executor = mock(TaskExecutor.class);
            when(executor.createThrottler(anyInt())).thenReturn(new DelayTaskThrottler(700));
            when(executor.execute(any())).thenAnswer(invocation -> {
                BackgroundTask<?> task = invocation.getArgument(0);
                return workers.submit(task::call);
            });
            SimpleBooleanProperty enabled = new SimpleBooleanProperty(false);
            FilePreferences preferences = mock(FilePreferences.class);
            when(preferences.fulltextIndexLinkedFilesProperty()).thenReturn(enabled);
            LuceneIndexer indexer = mock(LuceneIndexer.class);
            CountDownLatch started = new CountDownLatch(1);
            CompletableFuture<Void> release = new CompletableFuture<>();
            doAnswer(_ -> {
                started.countDown();
                release.join();
                return null;
            }).when(indexer).updateOnStart(any());
            LinkedFilesIndexManager manager = new LinkedFilesIndexManager(new BibDatabaseContext(), executor, preferences, indexer);
            try {
                enabled.set(true);
                assertTrue(started.await(5, TimeUnit.SECONDS));
                enabled.set(false);
                manager.close();
                release.complete(null);
                manager.closeAndWait();
                verify(indexer, times(2)).removeAllFromIndex();
                verify(indexer).closeAndWait();
            } finally {
                release.complete(null);
                manager.closeAndWait();
            }
        }
    }

    @Test
        // [utest->req~search.fulltext.disable-indexing~1]
    void reenablingHidesStaleIndexUntilItIsCleared() throws Exception {
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
             IndexWriter writer = new IndexWriter(new ByteBuffersDirectory(), new IndexWriterConfig(LinkedFilesConstants.LINKED_FILES_ANALYZER));
             SearcherManager searcherManager = new SearcherManager(writer, null)) {
            TaskExecutor executor = mock(TaskExecutor.class);
            when(executor.createThrottler(anyInt())).thenReturn(new DelayTaskThrottler(700));
            when(executor.execute(any())).thenAnswer(invocation -> {
                BackgroundTask<?> task = invocation.getArgument(0);
                return workers.submit(task::call);
            });
            SimpleBooleanProperty enabled = new SimpleBooleanProperty(false);
            FilePreferences preferences = mock(FilePreferences.class);
            when(preferences.fulltextIndexLinkedFilesProperty()).thenReturn(enabled);
            when(preferences.shouldFulltextIndexLinkedFiles()).thenAnswer(_ -> enabled.get());
            LuceneIndexer indexer = mock(LuceneIndexer.class);
            when(indexer.getSearcherManager()).thenReturn(searcherManager);
            doAnswer(_ -> {
                writer.deleteAll();
                return null;
            }).when(indexer).removeAllFromIndex();
            CountDownLatch staleIndexed = new CountDownLatch(1);
            CountDownLatch rebuilt = new CountDownLatch(1);
            CompletableFuture<Void> release = new CompletableFuture<>();
            AtomicInteger starts = new AtomicInteger();
            doAnswer(_ -> {
                if (starts.incrementAndGet() == 1) {
                    writer.addDocument(indexedPage("stale"));
                    staleIndexed.countDown();
                    release.join();
                } else {
                    writer.addDocument(indexedPage("fresh"));
                    rebuilt.countDown();
                }
                return null;
            }).when(indexer).updateOnStart(any());
            BibDatabaseContext databaseContext = new BibDatabaseContext();
            databaseContext.getDatabase().insertEntry(new BibEntry().withFiles(List.of(new LinkedFile("", "paper.pdf", "PDF"))));
            LinkedFilesIndexManager manager = new LinkedFilesIndexManager(databaseContext, executor, preferences, indexer);
            try {
                enabled.set(true);
                assertTrue(staleIndexed.await(5, TimeUnit.SECONDS));
                assertEquals(1, fulltextMatches(manager, "stale"));
                enabled.set(false);
                enabled.set(true);
                assertEquals(0, fulltextMatches(manager, "stale"));
                release.complete(null);
                assertTrue(rebuilt.await(5, TimeUnit.SECONDS));
                assertEquals(0, fulltextMatches(manager, "stale"));
                assertEquals(1, fulltextMatches(manager, "fresh"));
            } finally {
                release.complete(null);
                manager.closeAndWait();
            }
        }
    }

    private static Document indexedPage(String content) {
        Document document = new Document();
        document.add(new StringField(LinkedFilesConstants.PATH.toString(), "paper.pdf", Field.Store.YES));
        document.add(new StringField(LinkedFilesConstants.PAGE_NUMBER.toString(), "1", Field.Store.YES));
        document.add(new TextField(LinkedFilesConstants.CONTENT.toString(), content, Field.Store.YES));
        return document;
    }

    private static int fulltextMatches(LinkedFilesIndexManager manager, String term) {
        return manager.search(new SearchQuery(term, EnumSet.of(SearchFlags.FULLTEXT))).getMatchedEntries().size();
    }

    private static class PausedPreference extends SimpleBooleanProperty {
        private final int pauseOnRead;
        private final AtomicReference<@Nullable Thread> reader = new AtomicReference<>();
        private final AtomicInteger reads = new AtomicInteger();
        private final CountDownLatch readPaused = new CountDownLatch(1);
        private final CompletableFuture<Void> resume = new CompletableFuture<>();

        private PausedPreference() {
            this(1);
        }

        private PausedPreference(int pauseOnRead) {
            this.pauseOnRead = pauseOnRead;
        }

        @Override
        public boolean get() {
            boolean value = super.get();
            if (Thread.currentThread() == reader.get() && reads.incrementAndGet() == pauseOnRead) {
                readPaused.countDown();
                resume.join();
            }
            return value;
        }
    }
}
