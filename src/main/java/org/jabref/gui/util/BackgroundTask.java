package org.jabref.gui.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javafx.concurrent.Task;

/**
 * This class is essentially a wrapper around {@link Task}.
 * We cannot use {@link Task} directly since it runs certain update notifications on the JavaFX thread,
 * and so makes testing harder.
 * We take the opportunity and implement a fluid interface.
 *
 * @param <V> type of the return value of the task
 */
public class BackgroundTask<V> {
    private final Callable<V> callable;
    private Runnable onRunning;
    private Consumer<V> onSuccess;
    private Consumer<Exception> onException;

    private BackgroundTask(Callable<V> callable) {
        this.callable = callable;
    }

    public static <V> BackgroundTask<V> wrap(Callable<V> callable) {
        return new BackgroundTask<>(callable);
    }

    public BackgroundTask<V> onRunning(Runnable onRunning) {
        this.onRunning = onRunning;
        return this;
    }

    public BackgroundTask<V> onSuccess(Consumer<V> onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public V call() throws Exception {
        return callable.call();
    }

    public Runnable getOnRunning() {
        return onRunning;
    }

    public Consumer<V> getOnSuccess() {
        return onSuccess;
    }

    public Consumer<Exception> getOnException() {
        return onException;
    }

    public BackgroundTask<V> onFailure(Consumer<Exception> onException) {
        this.onException = onException;
        return this;
    }

    public void executeWith(TaskExecutor taskExecutor) {
        taskExecutor.execute(this);
    }
}
