package org.jabref.logic.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jabref.gui.JabRefExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows to throttle a list of tasks.
 * Use case: you have an event that occurs often, and every time you want to invoke the same task.
 * However, if a lot of events happen in a relatively short time span, then only one task should be invoked.
 *
 * @param <T>
 * @param <T>
 *
 * @implNote Once {@link #schedule(Runnable)} is called, the task is delayed for a given time span.
 *           If during this time, {@link #schedule(Runnable)} is called again, then the original task is canceled and the new one scheduled.
 */
public class DelayTaskThrottler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelayTaskThrottler.class);

    private final ScheduledThreadPoolExecutor executor;
    private final int delay;

    private ScheduledFuture<T> scheduledTask;

    /**
     * @param delay delay in milliseconds
     */
    public DelayTaskThrottler(int delay) {
        this.delay = delay;
        this.executor = new ScheduledThreadPoolExecutor(1);
        this.executor.setRemoveOnCancelPolicy(true);
        this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    public ScheduledFuture<T> scheduleTask(Callable<T> command) {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        try {
            scheduledTask = executor.schedule(command, delay, TimeUnit.MILLISECONDS);

        } catch (RejectedExecutionException e) {
            LOGGER.debug("Rejecting while another process is already running.");
        }
        return scheduledTask;
    }

    public ScheduledFuture<?> schedule(Runnable command) {
        return scheduleTask(Executors.callable(command, null));
    }

    /**
     * Shuts everything down. Upon termination, this method returns.
     */
    public void shutdown() {
        JabRefExecutorService.gracefullyShutdown(executor);
    }
}
