package org.jabref;

import java.util.ArrayList;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for managing of all threads (except Swing threads) in JabRef
 */
public class JabRefExecutorService implements Executor {

    public static final JabRefExecutorService INSTANCE = new JabRefExecutorService();
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefExecutorService.class);
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

    private JabRefExecutorService() {
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command);
        executorService.execute(command);
    }

    public void executeAndWait(Runnable command) {
        Objects.requireNonNull(command);
        Future<?> future = executorService.submit(command);
        while (true) {
            try {
                future.get();
                return;
            } catch (InterruptedException ignored) {
                // Ignored
            } catch (ExecutionException e) {
                LOGGER.error("Problem executing command", e);
            }
        }
    }

    public boolean executeAndWait(Callable<?> command) {
        Objects.requireNonNull(command);
        Future<?> future = executorService.submit(command);
        while (true) {
            try {
                future.get();
                return true;
            } catch (InterruptedException ignored) {
                // Ignored
            } catch (ExecutionException e) {
                LOGGER.error("Problem executing command", e);
                return false;
            }
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
        List<Future<T>> futures = new ArrayList<>();
        try {
            futures = executorService.invokeAll(tasks);
        } catch (InterruptedException exception) {
            LOGGER.error("Unable to execute tasks", exception);
            return Collections.emptyList();
        }
        return futures;
    }

    public void executeInterruptableTask(final Runnable runnable) {
        this.lowPriorityExecutorService.execute(runnable);
    }

    public void executeInterruptableTask(final Runnable runnable, String taskName) {
        this.lowPriorityExecutorService.execute(new NamedRunnable(taskName, runnable));
    }

    public void executeInterruptableTaskAndWait(Runnable runnable) {
        Objects.requireNonNull(runnable);

        Future<?> future = lowPriorityExecutorService.submit(runnable);
        while (true) {
            try {
                future.get();
                return;
            } catch (InterruptedException ignored) {
                // Ignored
            } catch (ExecutionException e) {
                LOGGER.error("Problem executing command", e);
            }
        }
    }

    public void manageRemoteThread(Thread thread) {
        if (this.remoteThread != null) {
            throw new IllegalStateException("Remote thread is already attached");
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

    public void shutdownEverything() {
        // those threads will be allowed to finish
        this.executorService.shutdown();
        //those threads will be interrupted in their current task
        this.lowPriorityExecutorService.shutdownNow();
        // kill the remote thread
        stopRemoteThread();
        // timer doesn't need to be canceled as it is run in daemon mode, which ensures that it is stopped if the application is shut down
    }

    class NamedRunnable implements Runnable {

        private final String name;

        private final Runnable task;

        public NamedRunnable(String name, Runnable runnable) {
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

}
