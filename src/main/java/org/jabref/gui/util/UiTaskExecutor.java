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
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.TaskExecutor;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A very simple implementation of the {@link TaskExecutor} interface.
 * Every submitted task is invoked in a separate thread.
 * <p>
 * In case something does not interact well with JavaFX, you can use the {@link HeadlessExecutorService}
 */
public class UiTaskExecutor implements TaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiTaskExecutor.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
    private final WeakHashMap<DelayTaskThrottler, Void> throttlers = new WeakHashMap<>();

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

    /**
     * This will convert the given {@link BackgroundTask} to a JavaFX {@link Task}
     * The JavaFX task executes the call method a background thread and the onFailed onSucceed on the FX UI thread
     *
     * @param task the BackgroundTask to run
     * @param <V> The background task type
     *
     * @return Future of a JavaFX Task which will execute the call method a background thread
     */
    @Override
    public <V> Future<V> execute(BackgroundTask<V> task) {
        Task<V> javafxTask = getJavaFXTask(task);
        if (task.showToUser()) {
            StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);
            if (stateManager != null) {
                stateManager.addBackgroundTask(task, javafxTask);
            } else {
                LOGGER.info("Background task visible without GUI");
            }
        }
        return execute(javafxTask);
    }

    /**
     * Runs the given task and returns a Future representing that task. Usually, you want to use the other method {@link
     * #execute(BackgroundTask)}.
     *
     * @param <V>  type of return value of the task
     * @param task the task to run
     */
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
        StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);
        if (stateManager != null) {
            stateManager.getRunningBackgroundTasks().forEach(Task::cancel);
        }
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

    /**
     * Generates a wrapper with a JavaFX {@link Task} for our BackgroundTask monitoring the progress based on the data given from the task.
     * <code>call</code> is routed to the given task object.
     *
     * @param task the BackgroundTask to wrap
     * @return a new Javafx Task object
     */
    public static <V> Task<V> getJavaFXTask(BackgroundTask<V> task) {
        Task<V> javaTask = new Task<>() {
            {
                this.updateMessage(task.messageProperty().get());
                this.updateTitle(task.titleProperty().get());
                BindingsHelper.subscribeFuture(task.progressProperty(), progress -> updateProgress(progress.workDone(), progress.max()));
                BindingsHelper.subscribeFuture(task.messageProperty(), this::updateMessage);
                BindingsHelper.subscribeFuture(task.titleProperty(), this::updateTitle);
                BindingsHelper.subscribeFuture(task.isCancelledProperty(), cancelled -> {
                    if (cancelled) {
                        cancel();
                    }
                });
                setOnCancelled(event -> task.cancel());
            }

            @Override
            protected V call() throws Exception {
                // this requires that background task call is public as it's in another package
                return task.call();
            }
        };
        Runnable onRunning = task.getOnRunning();
        if (onRunning != null) {
            javaTask.setOnRunning(event -> onRunning.run());
        }
        Consumer<V> onSuccess = task.getOnSuccess();
        javaTask.setOnSucceeded(event -> {
            // Set to 100% completed on completion
            task.updateProgress(1, 1);

            if (onSuccess != null) {
                onSuccess.accept(javaTask.getValue());
            }
        });
        Consumer<Exception> onException = task.getOnException();
        if (onException != null) {
            javaTask.setOnFailed(event -> onException.accept(convertToException(javaTask.getException())));
        }
        return javaTask;
    }

    private static Exception convertToException(Throwable throwable) {
        if (throwable instanceof Exception exception) {
            return exception;
        } else {
            return new Exception(throwable);
        }
    }
}
