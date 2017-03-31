package org.jabref;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Responsible for managing of all threads (except Swing threads) in JabRef
 */
public class JabRefExecutorService implements Executor {

    public static final JabRefExecutorService INSTANCE = new JabRefExecutorService();
    private static final Log LOGGER = LogFactory.getLog(JabRefExecutorService.class);
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

    private JabRefExecutorService() {}

    @Override
    public void execute(Runnable command) {
        if (command == null) {
            LOGGER.debug("Received null as command for execution");
            return;
        }

        executorService.execute(command);
    }

    public void executeAndWait(Runnable command) {
        if (command == null) {
            LOGGER.debug("Received null as command for execution");
            return;
        }

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

    public void executeInterruptableTask(final Runnable runnable) {
        this.lowPriorityExecutorService.execute(runnable);
    }

    public void executeInterruptableTask(final Runnable runnable, String taskName) {
        this.lowPriorityExecutorService.execute(new NamedRunnable(taskName, runnable));
    }

    public void executeInterruptableTaskAndWait(Runnable runnable) {
        if(runnable == null) {
            LOGGER.debug("Received null as command for execution");
            return;
        }

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
        if (this.remoteThread != null){
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
