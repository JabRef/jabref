package org.jabref.gui.util;

import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.jabref.gui.StateManager;
import org.jabref.logic.util.DelayTaskThrottler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple implementation of the {@link TaskExecutor} interface.
 * Every submitted task is invoked in a separate thread.
 */
public class DefaultTaskExecutor implements TaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTaskExecutor.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
    private final WeakHashMap<DelayTaskThrottler, Void> throttlers = new WeakHashMap<>();

    private final StateManager stateManager;

    public DefaultTaskExecutor(StateManager stateManager) {
        super();
        this.stateManager = stateManager;
    }

    /**
     *
     */
    public static <V> V runInJavaFXThread(Callable<V> callable) {
        if (Platform.isFxApplicationThread()) {
            try {
                return callable.call();
            } catch (Exception e) {
                LOGGER.error("Problem executing call", e);
                return null;
            }
        }

        FutureTask<V> task = new FutureTask<>(callable);

        Platform.runLater(task);

        try {
            return task.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Problem running in fx thread", e);
            return null;
        }
    }

    /**
     * Runs the specified {@link Runnable} on the JavaFX application thread and waits for completion.
     *
     * @param action the {@link Runnable} to run
     * @throws NullPointerException if {@code action} is {@code null}
     */
    public static void runAndWaitInJavaFXThread(Runnable action) {
        Objects.requireNonNull(action);

        // Run synchronously on JavaFX thread
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        // Queue on JavaFX thread and wait for completion
        final CountDownLatch doneLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                doneLatch.countDown();
            }
        });

        try {
            doneLatch.await();
        } catch (InterruptedException e) {
            LOGGER.error("Problem running action on JavaFX thread", e);
        }
    }

    public static void runInJavaFXThread(Runnable runnable) {
        Platform.runLater(runnable);
    }

    @Override
    public <V> Future<V> execute(BackgroundTask<V> task) {
        Task<V> javafxTask = getJavaFXTask(task);
        if (task.showToUser()) {
            stateManager.addBackgroundTask(javafxTask);
        }
        return execute(javafxTask);
    }

    @Override
    public <V> Future<V> execute(Task<V> task) {
        executor.submit(task);
        return task;
    }

    @Override
    public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
        return scheduledExecutor.schedule(getJavaFXTask(task), delay, unit);
    }

    /**
     * Shuts everything down. After termination, this method returns.
     */
    @Override
    public void shutdown() {
        stateManager.getBackgroundTasks().stream().filter(task -> !task.isDone()).forEach(Task::cancel);
        executor.shutdownNow();
        scheduledExecutor.shutdownNow();
        throttlers.forEach((throttler, aVoid) -> throttler.shutdown());
    }

    @Override
    public DelayTaskThrottler createThrottler(int delay) {
        DelayTaskThrottler throttler = new DelayTaskThrottler(delay);
        throttlers.put(throttler, null);
        return throttler;
    }

    private <V> Task<V> getJavaFXTask(BackgroundTask<V> task) {
        Task<V> javaTask = new Task<V>() {

            {
                this.updateMessage(task.messageProperty().get());
                this.updateTitle(task.titleProperty().get());
                BindingsHelper.subscribeFuture(task.progressProperty(), progress -> updateProgress(progress.getWorkDone(), progress.getMax()));
                BindingsHelper.subscribeFuture(task.messageProperty(), this::updateMessage);
                BindingsHelper.subscribeFuture(task.titleProperty(), this::updateTitle);
                BindingsHelper.subscribeFuture(task.isCanceledProperty(), cancelled -> {
                    if (cancelled) {
                        cancel();
                    }
                });
                setOnCancelled(event -> task.cancel());
            }

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
