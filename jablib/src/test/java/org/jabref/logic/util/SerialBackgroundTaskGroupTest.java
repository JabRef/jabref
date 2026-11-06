package org.jabref.logic.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class SerialBackgroundTaskGroupTest {
    @Test
    void cancelDiscardsQueuedWorkAndAllowsNewWork() throws Exception {
        TaskExecutor executor = mock(TaskExecutor.class);
        List<BackgroundTask<?>> queued = new ArrayList<>();
        when(executor.execute(any())).thenAnswer(invocation -> {
            queued.add(invocation.getArgument(0));
            return CompletableFuture.completedFuture(null);
        });
        SerialBackgroundTaskGroup group = new SerialBackgroundTaskGroup(executor);
        AtomicBoolean staleRan = new AtomicBoolean();
        AtomicBoolean freshRan = new AtomicBoolean();
        group.submit(_ -> staleRan.set(true));
        group.cancel();
        group.submit(_ -> freshRan.set(true));
        queued.getFirst().call();
        queued.getLast().call();
        assertFalse(staleRan.get());
        assertTrue(freshRan.get());
    }

    @Test
    void closeWaitsForWorkerToExitAfterCancellation() throws Exception {
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            TaskExecutor executor = mock(TaskExecutor.class);
            when(executor.execute(any())).thenAnswer(invocation -> {
                BackgroundTask<?> task = invocation.getArgument(0);
                return workers.submit(task::call);
            });
            SerialBackgroundTaskGroup group = new SerialBackgroundTaskGroup(executor);
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch cancelled = new CountDownLatch(1);
            CompletableFuture<Void> release = new CompletableFuture<>();
            AtomicBoolean closed = new AtomicBoolean();
            try {
                group.submit(task -> {
                    task.isCancelledProperty().addListener((_, _, value) -> {
                        if (value) {
                            cancelled.countDown();
                        }
                    });
                    started.countDown();
                    release.join();
                });
                assertTrue(started.await(5, TimeUnit.SECONDS));
                Future<?> close = workers.submit(() -> group.closeAndWait(() -> closed.set(true)));
                assertTrue(cancelled.await(5, TimeUnit.SECONDS));
                assertFalse(closed.get());
                release.complete(null);
                close.get(5, TimeUnit.SECONDS);
                assertTrue(closed.get());
                AtomicBoolean afterClose = new AtomicBoolean();
                group.submit(_ -> afterClose.set(true));
                assertFalse(afterClose.get());
            } finally {
                release.complete(null);
            }
        }
    }

    @Test
    void asynchronousCloseRejectsWorkAndWaitsForCleanup() throws Exception {
        TaskExecutor executor = mock(TaskExecutor.class);
        SerialBackgroundTaskGroup group = new SerialBackgroundTaskGroup(executor);
        CountDownLatch cleanupStarted = new CountDownLatch(1);
        CompletableFuture<Void> releaseCleanup = new CompletableFuture<>();
        try (ExecutorService waiters = Executors.newSingleThreadExecutor()) {
            try {
                group.close(() -> {
                    cleanupStarted.countDown();
                    releaseCleanup.join();
                });
                assertTrue(cleanupStarted.await(5, TimeUnit.SECONDS));
                group.submit(_ -> {
                });
                verify(executor, never()).execute(any());
                Future<?> waiting = waiters.submit(() -> group.closeAndWait(() -> {
                }));
                assertFalse(waiting.isDone());
                releaseCleanup.complete(null);
                waiting.get(5, TimeUnit.SECONDS);
            } finally {
                releaseCleanup.complete(null);
            }
        }
    }

    @Test
    void submitsOnlyOneTaskToExecutorAtATime() throws Exception {
        TaskExecutor executor = mock(TaskExecutor.class);
        List<BackgroundTask<?>> submitted = new ArrayList<>();
        when(executor.execute(any())).thenAnswer(invocation -> {
            submitted.add(invocation.getArgument(0));
            return CompletableFuture.completedFuture(null);
        });
        SerialBackgroundTaskGroup group = new SerialBackgroundTaskGroup(executor);
        group.submit(_ -> {
        });
        group.submit(_ -> {
        });
        assertEquals(1, submitted.size());
        submitted.getFirst().call();
        assertEquals(2, submitted.size());
        submitted.getLast().call();
    }

    @Test
    void cancelAfterCloseDoesNotDiscardPendingCleanup() throws Exception {
        try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
            TaskExecutor executor = mock(TaskExecutor.class);
            when(executor.execute(any())).thenAnswer(invocation -> {
                BackgroundTask<?> task = invocation.getArgument(0);
                return workers.submit(task::call);
            });
            SerialBackgroundTaskGroup group = new SerialBackgroundTaskGroup(executor);
            CountDownLatch started = new CountDownLatch(1);
            CompletableFuture<Void> release = new CompletableFuture<>();
            AtomicBoolean cleanupRan = new AtomicBoolean();
            try {
                group.submit(_ -> {
                    started.countDown();
                    release.join();
                });
                assertTrue(started.await(5, TimeUnit.SECONDS));
                group.cancelAndRunWhenIdle(() -> cleanupRan.set(true));
                group.close(() -> {
                });
                group.cancel();
                release.complete(null);
                group.closeAndWait(() -> {
                });
                assertTrue(cleanupRan.get());
            } finally {
                release.complete(null);
            }
        }
    }

    @Test
    void submissionWithCancelledGenerationIsDiscarded() {
        TaskExecutor executor = mock(TaskExecutor.class);
        SerialBackgroundTaskGroup group = new SerialBackgroundTaskGroup(executor);
        long generation = group.generation();
        group.cancel();
        group.submit(generation, _ -> {
        });
        verify(executor, never()).execute(any());
    }

    @Test
    void synchronousExecutorDrainsLongQueueWithoutNesting() {
        SerialBackgroundTaskGroup group = new SerialBackgroundTaskGroup(new CurrentThreadTaskExecutor());
        int queuedTasks = 50_000;
        AtomicInteger completed = new AtomicInteger();
        group.submit(_ -> {
            for (int i = 0; i < queuedTasks; i++) {
                group.submit(_ -> completed.incrementAndGet());
            }
        });
        assertEquals(queuedTasks, completed.get());
    }
}
