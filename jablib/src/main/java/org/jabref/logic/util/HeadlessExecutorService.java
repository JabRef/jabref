package org.jabref.logic.util;

import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Responsible for managing of all threads (_except_ GUI threads) in JabRef.
///
/// GUI background tasks should run in `org.jabref.gui.util.UiTaskExecutor``
///
/// This is a wrapper around [ExecutorService]
///
/// Offers both high-priority and low-priority thread pools.
public class HeadlessExecutorService implements Executor {

    public static final HeadlessExecutorService INSTANCE = new HeadlessExecutorService();

    private static final Logger LOGGER = LoggerFactory.getLogger(HeadlessExecutorService.class);

    private static final String EXECUTOR_NAME = "JabRef CachedThreadPool";
    private static final String LOW_PRIORITY_EXECUTOR_NAME = "JabRef LowPriorityCachedThreadPool";

    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName(EXECUTOR_NAME);
        thread.setUncaughtExceptionHandler(new FallbackExceptionHandler());
        return thread;
    });

    private final ExecutorService lowPriorityExecutorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName(LOW_PRIORITY_EXECUTOR_NAME);
        thread.setUncaughtExceptionHandler(new FallbackExceptionHandler());
        return thread;
    });

    private final Timer timer = new Timer("timer", true);

    private HeadlessExecutorService() {
    }

    public void execute(@NonNull Runnable command) {
        executorService.execute(command);
    }

    public void executeAndWait(@NonNull Runnable command) {
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
    public <T> Future<T> execute(@NonNull Callable<T> command) {
        return executorService.submit(command);
    }

    /**
     * Executes a collection of callable tasks and returns a List of the resulting Future objects after the calculation is done.
     *
     * @param tasks The tasks to execute
     * @return A List of Future objects that provide the returning values.
     */
    public <T> List<Future<T>> executeAll(@NonNull Collection<Callable<T>> tasks) {
        try {
            return executorService.invokeAll(tasks);
        } catch (InterruptedException exception) {
            // Ignored
            return List.of();
        }
    }

    public <T> List<Future<T>> executeAll(@NonNull Collection<Callable<T>> tasks, int timeout, TimeUnit timeUnit) {
        try {
            return executorService.invokeAll(tasks, timeout, timeUnit);
        } catch (InterruptedException exception) {
            // Ignored
            return List.of();
        }
    }

    public void executeInterruptableTask(final Runnable runnable, String taskName) {
        this.lowPriorityExecutorService.execute(new NamedRunnable(taskName, runnable));
    }

    public void executeInterruptableTaskAndWait(@NonNull Runnable runnable) {
        Future<?> future = lowPriorityExecutorService.submit(runnable);
        try {
            future.get();
        } catch (InterruptedException e) {
            LOGGER.error("The thread is waiting, occupied or interrupted", e);
        } catch (ExecutionException e) {
            LOGGER.error("Problem executing command", e);
        }
    }

    public void submit(TimerTask timerTask, long millisecondsDelay) {
        timer.schedule(timerTask, millisecondsDelay);
    }

    /**
     * Shuts everything down. After termination, this method returns.
     */
    public void shutdownEverything() {
        LOGGER.trace("Gracefully shut down executor service");
        gracefullyShutdown(EXECUTOR_NAME, this.executorService, 15);

        LOGGER.trace("Gracefully shut down low priority executor service");
        gracefullyShutdown(LOW_PRIORITY_EXECUTOR_NAME, this.lowPriorityExecutorService, 15);

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
    public static void gracefullyShutdown(String name, ExecutorService executorService, int timeoutInSeconds) {
        try {
            // This is non-blocking. See https://stackoverflow.com/a/57383461/873282.
            executorService.shutdown();
            if (!executorService.awaitTermination(timeoutInSeconds, TimeUnit.SECONDS)) {
                LOGGER.debug("{} seconds passed, {} still not completed. Trying forced shutdown.", timeoutInSeconds, name);
                // those threads will be interrupted in their current task
                executorService.shutdownNow();
                if (executorService.awaitTermination(timeoutInSeconds, TimeUnit.SECONDS)) {
                    LOGGER.debug("{} seconds passed again - forced shutdown of {} worked.", timeoutInSeconds, name);
                } else {
                    LOGGER.error("{} did not terminate", name);
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
