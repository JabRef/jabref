package org.jabref.logic.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jabref.gui.FallbackExceptionHandler;
import org.jabref.gui.util.UiTaskExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for managing of all threads (<em>except</em> GUI threads) in JabRef.
 * <p>
 * GUI background tasks should run in {@link UiTaskExecutor}.
 * <p>
 * This is a wrapper around {@link ExecutorService}
 * <p>
 *     Offers both high-priority and low-priority thread pools.
 * </p>
 */
public class HeadlessExecutorService implements Executor {

    public static final HeadlessExecutorService INSTANCE = new HeadlessExecutorService();

    private static final Logger LOGGER = LoggerFactory.getLogger(HeadlessExecutorService.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("JabRef CachedThreadPool");
        thread.setUncaughtExceptionHandler(new FallbackExceptionHandler());
        return thread;
    });

    private final ExecutorService lowPriorityExecutorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("JabRef LowPriorityCachedThreadPool");
        thread.setUncaughtExceptionHandler(new FallbackExceptionHandler());
        return thread;
    });

    private final Timer timer = new Timer("timer", true);

    private Thread remoteThread;

    private HeadlessExecutorService() {
   }

    public void execute(Runnable command) {
        Objects.requireNonNull(command);
        executorService.execute(command);
    }

    public void executeAndWait(Runnable command) {
        Objects.requireNonNull(command);
        Future<?> future = executorService.submit(command);
        try {
            future.get();
        } catch (InterruptedException e) {
            LOGGER.debug("The thread is waiting, occupied or interrupted", e);
        } catch (ExecutionException e) {
            LOGGER.error("Problem executing command", e);
        }
    }

    /**
     * Executes a callable task that provides a return value after the calculation is done.
     *
     * @param command The task to execute.
     * @return A Future object that provides the returning value.
     */
    public <T> Future<T> execute(Callable<T> command) {
        Objects.requireNonNull(command);
        return executorService.submit(command);
    }

    /**
     * Executes a collection of callable tasks and returns a List of the resulting Future objects after the calculation is done.
     *
     * @param tasks The tasks to execute
     * @return A List of Future objects that provide the returning values.
     */
    public <T> List<Future<T>> executeAll(Collection<Callable<T>> tasks) {
        Objects.requireNonNull(tasks);
        try {
            return executorService.invokeAll(tasks);
        } catch (InterruptedException exception) {
            // Ignored
            return Collections.emptyList();
        }
    }

    public <T> List<Future<T>> executeAll(Collection<Callable<T>> tasks, int timeout, TimeUnit timeUnit) {
        Objects.requireNonNull(tasks);
        try {
            return executorService.invokeAll(tasks, timeout, timeUnit);
        } catch (InterruptedException exception) {
            // Ignored
            return Collections.emptyList();
        }
    }

    public void executeInterruptableTask(final Runnable runnable, String taskName) {
        this.lowPriorityExecutorService.execute(new NamedRunnable(taskName, runnable));
    }

    public void executeInterruptableTaskAndWait(Runnable runnable) {
        Objects.requireNonNull(runnable);

        Future<?> future = lowPriorityExecutorService.submit(runnable);
        try {
            future.get();
        } catch (InterruptedException e) {
            LOGGER.error("The thread is waiting, occupied or interrupted", e);
        } catch (ExecutionException e) {
            LOGGER.error("Problem executing command", e);
        }
    }

    public void startRemoteThread(Thread thread) {
        if (this.remoteThread != null) {
            throw new IllegalStateException("Tele thread is already attached");
        } else {
            this.remoteThread = thread;
            remoteThread.start();
        }
    }

    public void stopRemoteThread() {
        if (remoteThread != null) {
            remoteThread.interrupt();
            remoteThread = null;
        }
    }

    public void submit(TimerTask timerTask, long millisecondsDelay) {
        timer.schedule(timerTask, millisecondsDelay);
    }

    /**
     * Shuts everything down. After termination, this method returns.
     */
    public void shutdownEverything() {
        LOGGER.trace("Stopping remote thread");
        stopRemoteThread();

        LOGGER.trace("Gracefully shut down executor service");
        gracefullyShutdown(this.executorService);

        LOGGER.trace("Gracefully shut down low priority executor service");
        gracefullyShutdown(this.lowPriorityExecutorService);

        LOGGER.trace("Canceling timer");
        timer.cancel();

        LOGGER.trace("Finished shutdownEverything");
    }

    private static class NamedRunnable implements Runnable {

        private final String name;

        private final Runnable task;

        private NamedRunnable(String name, Runnable runnable) {
            this.name = name;
            this.task = runnable;
        }

        @Override
        public void run() {
            final String orgName = Thread.currentThread().getName();
            Thread.currentThread().setName(name);
            try {
                task.run();
            } finally {
                Thread.currentThread().setName(orgName);
            }
        }
    }

    /**
     * Shuts down the provided executor service by first trying a normal shutdown, then waiting for the shutdown and then forcibly shutting it down.
     * Returns if the status of the shut down is known.
     */
    public static void gracefullyShutdown(ExecutorService executorService) {
        try {
            // This is non-blocking. See https://stackoverflow.com/a/57383461/873282.
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                LOGGER.debug("One minute passed, {} still not completed. Trying forced shutdown.", executorService.toString());
                // those threads will be interrupted in their current task
                executorService.shutdownNow();
                if (executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    LOGGER.debug("One minute passed again - forced shutdown of {} worked.", executorService.toString());
                } else {
                    LOGGER.error("{} did not terminate", executorService.toString());
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
