package org.jabref.gui.util;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javafx.concurrent.Task;

import org.jabref.logic.util.DelayTaskThrottler;

/**
 * An object that executes submitted {@link Task}s. This
 * interface provides a way of decoupling task submission from the
 * mechanics of how each task will be run, including details of thread
 * use, scheduling, thread pooling, etc.
 */
public interface TaskExecutor {

    /**
     * Runs the given task and returns a Future representing that task.
     *
     * @param <V>  type of return value of the task
     * @param task the task to run
     */
    <V> Future<V> execute(BackgroundTask<V> task);

    /**
     * Runs the given task and returns a Future representing that task. Usually, you want to use the other method {@link
     * #execute(BackgroundTask)}.
     *
     * @param <V>  type of return value of the task
     * @param task the task to run
     */
    <V> Future<V> execute(Task<V> task);

    /**
     * Submits a one-shot task that becomes enabled after the given delay.
     *
     * @param task  the task to execute
     * @param delay the time from now to delay execution
     * @param unit  the time unit of the delay parameter
     * @return a ScheduledFuture representing pending completion of
     *         the task and whose {@code get()} method will return
     *         {@code null} upon completion
     */
    <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit);

    /**
     * Shutdown the task executor. May happen in the background or may be finished when this method returns.
     */
    void shutdown();

    /**
     * Creates a new task throttler, and registers it so that it gets properly shutdown.
     */
    DelayTaskThrottler createThrottler(int delay);
}
