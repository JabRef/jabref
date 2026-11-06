package org.jabref.logic.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
            AtomicReference<BackgroundTask<?>> firstTask = new AtomicReference<>();
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
                assertTrue(Optional.ofNullable(firstTask.get()).orElseThrow().isCancelled());
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
}
