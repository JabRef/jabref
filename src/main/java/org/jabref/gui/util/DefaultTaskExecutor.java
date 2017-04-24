package org.jabref.gui.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A very simple implementation of the {@link TaskExecutor} interface.
 * Every submitted task is invoked in a separate thread.
 */
public class DefaultTaskExecutor implements TaskExecutor {

    private static final Log LOGGER = LogFactory.getLog(DefaultTaskExecutor.class);

    private ExecutorService executor = Executors.newFixedThreadPool(5);

    public static <V> V runInJavaFXThread(Callable<V> callable) {
        FutureTask<V> task = new FutureTask<>(callable);
        Platform.runLater(task);
        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e);
            return null;
        }
    }

    public static void runInJavaFXThread(Runnable runnable) {
        Platform.runLater(runnable);
    }

    @Override
    public <V> void execute(BackgroundTask<V> task) {
        executor.submit(getJavaFXTask(task));
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }

    private <V> Task<V> getJavaFXTask(BackgroundTask<V> task) {
        Task<V> javaTask = new Task<V>() {

            @Override
            public V call() throws Exception {
                return task.call();
            }
        };
        Runnable onRunning = task.getOnRunning();
        if (onRunning != null) {
            javaTask.setOnRunning(event -> onRunning.run());
        }
        Consumer<V> onSuccess = task.getOnSuccess();
        if (onSuccess != null) {
            javaTask.setOnSucceeded(event -> onSuccess.accept(javaTask.getValue()));
        }
        Consumer<Exception> onException = task.getOnException();
        if (onException != null) {
            javaTask.setOnFailed(event -> onException.accept(convertToException(javaTask.getException())));
        }
        return javaTask;
    }

    private Exception convertToException(Throwable throwable) {
        if (throwable instanceof Exception) {
            return (Exception) throwable;
        } else {
            return new Exception(throwable);
        }
    }
}
