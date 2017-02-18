package org.jabref.gui.util;

import java.util.function.Consumer;

import javafx.concurrent.Task;

/**
 * A very simple implementation of the {@link TaskExecutor} interface.
 * Every submitted task is invoked in a separate thread.
 */
public class DefaultTaskExecutor implements TaskExecutor {

    @Override
    public <V> void execute(BackgroundTask<V> task) {
        new Thread(getJavaFXTask(task)).start();
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
