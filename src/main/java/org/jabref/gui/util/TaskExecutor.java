package org.jabref.gui.util;

import javafx.concurrent.Task;

/**
 * An object that executes submitted {@link Task}s. This
 * interface provides a way of decoupling task submission from the
 * mechanics of how each task will be run, including details of thread
 * use, scheduling, thread pooling, etc.
 */
public interface TaskExecutor {

    /**
     * Runs the given task.
     *
     * @param task the task to run
     * @param <V>  type of return value of the task
     */
    <V> void execute(BackgroundTask<V> task);

    /**
     * Shutdown the task executor.
     */
    void shutdown();
}
