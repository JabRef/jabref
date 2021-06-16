package org.jabref.gui.util;

import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javafx.concurrent.Task;

import org.jabref.logic.util.DelayTaskThrottler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link TaskExecutor} that runs every task on the current thread, i.e. in a sequential order. This
 * class is not designed to be used in production but should make code involving asynchronous operations deterministic
 * and testable.
 */
public class CurrentThreadTaskExecutor implements TaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentThreadTaskExecutor.class);
    private final WeakHashMap<DelayTaskThrottler, Void> throttlers = new WeakHashMap<>();

    /**
     * Executes the task on the current thread. The code is essentially taken from {@link
     * javafx.concurrent.Task.TaskCallable#call()}, but adapted to run sequentially.
     */
    @Override
    public <V> Future<V> execute(BackgroundTask<V> task) {
        Runnable onRunning = task.getOnRunning();
        if (onRunning != null) {
            onRunning.run();
        }
        try {
            final V result = task.call();
            Consumer<V> onSuccess = task.getOnSuccess();
            if (onSuccess != null) {
                onSuccess.accept(result);
            }
            return CompletableFuture.completedFuture(result);
        } catch (Exception exception) {
            Consumer<Exception> onException = task.getOnException();
            if (onException != null) {
                onException.accept(exception);
            } else {
                LOGGER.error("Unhandled exception", exception);
            }
            return new FailedFuture<>(exception);
        }
    }

    @Override
    public <V> Future<V> execute(Task<V> task) {
        return task;
    }

    @Override
    public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
        return execute(task);
    }

    @Override
    public void shutdown() {
        throttlers.forEach((throttler, aVoid) -> throttler.shutdown());
    }

    @Override
    public DelayTaskThrottler createThrottler(int delay) {
        DelayTaskThrottler throttler = new DelayTaskThrottler(delay);
        throttlers.put(throttler, null);
        return throttler;
    }

    private static class FailedFuture<T> implements Future<T> {
        private final Throwable exception;

        FailedFuture(Throwable exception) {
            this.exception = exception;
        }

        @Override
        public T get() throws ExecutionException {
            throw new ExecutionException(exception);
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws ExecutionException {
            return get();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }
    }
}
