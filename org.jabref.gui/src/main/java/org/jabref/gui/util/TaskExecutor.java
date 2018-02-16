package org.jabref.gui.util;

import java.util.concurrent.Future;

import javafx.concurrent.Task;

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
    <V> Future<?> execute(BackgroundTask<V> task);

    /**
     * Shutdown the task executor.
     */
    void shutdown();
}
