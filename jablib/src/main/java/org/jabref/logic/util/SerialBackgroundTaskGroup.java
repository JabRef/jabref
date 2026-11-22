package org.jabref.logic.util;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Serializes tasks that share a resource and invalidates queued tasks when cancelled.
/// Cancellation is cooperative; a running task retains exclusive access until its call returns.
@NullMarked
public class SerialBackgroundTaskGroup {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialBackgroundTaskGroup.class);
    private final TaskExecutor executor;
    private final CompletableFuture<Void> closeCompletion = new CompletableFuture<>();
    private final Object stateLock = new Object();
    private final Object workLock = new Object();
    private final Queue<Work> pending = new ArrayDeque<>();
    private final ThreadLocal<@Nullable SchedulingState> schedulingOnThisThread = new ThreadLocal<>();
    private @Nullable BackgroundTask<?> activeTask;
    private @Nullable Runnable pendingCleanup;
    private long generation;
    private boolean closed;
    private boolean scheduled;

    private record Work(long generation, Consumer<BackgroundTask<?>> action) {
    }

    private static final class SchedulingState {
        private boolean rescheduleRequested;
    }

    public SerialBackgroundTaskGroup(TaskExecutor executor) {
        this.executor = executor;
    }

    /// Submits work unless the group has closed. Work from a cancelled generation is skipped.
    public void submit(Consumer<BackgroundTask<?>> work) {
        enqueue(0, true, work);
    }

    /// Returns a token invalidated by cancellation or closure.
    public long generation() {
        synchronized (stateLock) {
            return generation;
        }
    }

    /// Submits work only if the token still identifies the current generation.
    public void submit(long expectedGeneration, Consumer<BackgroundTask<?>> work) {
        enqueue(expectedGeneration, false, work);
    }

    private void enqueue(long expectedGeneration, boolean acceptCurrent, Consumer<BackgroundTask<?>> work) {
        boolean schedule;
        synchronized (stateLock) {
            if (closed || (!acceptCurrent && generation != expectedGeneration)) {
                return;
            }
            pending.add(new Work(generation, work));
            schedule = !scheduled;
            if (schedule) {
                scheduled = true;
            }
        }
        if (schedule) {
            scheduleNext();
        }
    }

    /// Drains iteratively instead of recursively: when the executor runs the task synchronously, the task's request to
    /// schedule its successor is recorded and handled after the task returns, keeping the stack depth constant.
    private void scheduleNext() {
        SchedulingState activeState = schedulingOnThisThread.get();
        if (activeState != null) {
            activeState.rescheduleRequested = true;
            return;
        }
        SchedulingState state = new SchedulingState();
        schedulingOnThisThread.set(state);
        try {
            state.rescheduleRequested = true;
            while (state.rescheduleRequested) {
                state.rescheduleRequested = false;
                scheduleOne();
            }
        } finally {
            schedulingOnThisThread.remove();
        }
    }

    private void scheduleOne() {
        Work work;
        synchronized (stateLock) {
            if (closed || pending.isEmpty()) {
                scheduled = false;
                return;
            }
            work = pending.remove();
        }
        BackgroundTask<Void> task = new BackgroundTask<>() {
            private volatile boolean cancellationRequested;

            @Override
            public void cancel(boolean mayInterruptIfRunning) {
                cancellationRequested = true;
                super.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return cancellationRequested || super.isCancelled();
            }

            @Override
            public Void call() {
                try {
                    synchronized (workLock) {
                        synchronized (stateLock) {
                            if (closed || generation != work.generation() || isCancelled()) {
                                return null;
                            }
                            activeTask = this;
                        }
                        try {
                            work.action().accept(this);
                        } finally {
                            synchronized (stateLock) {
                                activeTask = null;
                            }
                        }
                    }
                    return null;
                } finally {
                    scheduleNext();
                }
            }
        };
        try {
            task.executeWith(executor);
        } catch (RejectedExecutionException exception) {
            LOGGER.warn("Could not schedule background work", exception);
            synchronized (stateLock) {
                pending.clear();
                scheduled = false;
            }
            throw exception;
        }
    }

    /// Invalidates queued work and requests cancellation without interrupting the active worker.
    public void cancel() {
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            pendingCleanup = null;
            cancelWork();
        }
    }

    /// Cancels the current generation and runs cleanup after active work exits, unless superseded.
    public void cancelAndRunWhenIdle(Runnable cleanup) {
        long cancelledGeneration;
        synchronized (stateLock) {
            if (closed) {
                return;
            }
            cancel();
            pendingCleanup = cleanup;
            cancelledGeneration = generation;
        }
        HeadlessExecutorService.INSTANCE.execute(() -> {
            synchronized (workLock) {
                Runnable cleanupToRun;
                synchronized (stateLock) {
                    if (closed || generation != cancelledGeneration) {
                        return;
                    }
                    if (pendingCleanup == null) {
                        return;
                    }
                    cleanupToRun = pendingCleanup;
                    pendingCleanup = null;
                }
                cleanupToRun.run();
            }
        });
    }

    /// Closes the group immediately and performs cleanup after active work exits.
    public void close(Runnable cleanup) {
        if (beginClose()) {
            try {
                HeadlessExecutorService.INSTANCE.execute(() -> finishClose(cleanup));
            } catch (RejectedExecutionException exception) {
                LOGGER.warn("Could not schedule background task group cleanup", exception);
                closeCompletion.completeExceptionally(exception);
                throw exception;
            }
        }
    }

    /// Closes the group and waits for cleanup, including an earlier asynchronous close.
    /// Do not call this from a group task or its cleanup callback.
    public void closeAndWait(Runnable cleanup) {
        if (beginClose()) {
            finishClose(cleanup);
        } else {
            closeCompletion.join();
        }
    }

    private boolean beginClose() {
        synchronized (stateLock) {
            if (closed) {
                return false;
            }
            closed = true;
            cancelWork();
            return true;
        }
    }

    private void cancelWork() {
        generation++;
        pending.clear();
        if (activeTask != null) {
            activeTask.cancel(false);
        }
    }

    private @Nullable Runnable takePendingCleanup() {
        synchronized (stateLock) {
            @Nullable Runnable cleanup = pendingCleanup;
            pendingCleanup = null;
            return cleanup;
        }
    }

    private void finishClose(Runnable cleanup) {
        try {
            synchronized (workLock) {
                @Nullable Runnable cleanupToRun = takePendingCleanup();
                try {
                    if (cleanupToRun != null) {
                        cleanupToRun.run();
                    }
                } finally {
                    cleanup.run();
                }
            }
            closeCompletion.complete(null);
        } catch (RuntimeException | Error exception) {
            LOGGER.warn("Background task group cleanup failed", exception);
            closeCompletion.completeExceptionally(exception);
            throw exception;
        }
    }
}
