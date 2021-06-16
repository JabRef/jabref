package org.jabref.logic.util;

import java.util.concurrent.Callable;
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
 * @param <T>
 *
 * @implNote Once {@link #schedule(Runnable)} is called, the task is delayed for a given time span.
 *         If during this time, {@link #schedule(Runnable)} is called again, then the original task is canceled and the new one scheduled.
 */
public class DelayTaskThrottler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelayTaskThrottler.class);

    private final ScheduledThreadPoolExecutor executor;

    private int delay;

    private ScheduledFuture<?> scheduledTask;

    /**
     * @param delay delay in milliseconds
     */
    public DelayTaskThrottler(int delay) {
        this.delay = delay;
        this.executor = new ScheduledThreadPoolExecutor(1);
        this.executor.setRemoveOnCancelPolicy(true);
        this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    public ScheduledFuture<?> schedule(Runnable command) {
        if (scheduledTask != null) {
            cancel();
        }
        try {
            scheduledTask = executor.schedule(command, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            LOGGER.debug("Rejecting while another process is already running.");
        }
        return scheduledTask;
    }

    // Execute scheduled Runnable early
    public void execute(Runnable command) {
        delay = 0;
        schedule(command);
    }

    // Cancel scheduled Runnable gracefully
    public void cancel() {
        scheduledTask.cancel(false);
    }

    public <T> ScheduledFuture<?> scheduleTask(Callable<?> command) {
        if (scheduledTask != null) {
            cancel();
        }
        try {
            scheduledTask = executor.schedule(command, delay, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            LOGGER.debug("Rejecting while another process is already running.");
        }
        return scheduledTask;
    }

    /**
     * Shuts everything down. Upon termination, this method returns.
     */
    public void shutdown() {
        JabRefExecutorService.gracefullyShutdown(executor);
    }
}
