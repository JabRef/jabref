package net.sf.jabref;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.sf.jabref.gui.undo.UndoableInsertEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Responsible for managing of all threads (except Swing threads) in JabRef
 */
public class JabRefExecutorService implements Executor {

    private static final Log LOGGER = LogFactory.getLog(UndoableInsertEntry.class);

    public static final JabRefExecutorService INSTANCE = new JabRefExecutorService();

    private final ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("JabRef CachedThreadPool");
        return thread;
    });

    private final ExecutorService lowPriorityExecutorService = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("JabRef LowPriorityCachedThreadPool");
        return thread;
    });

    private final Timer timer = new Timer("timer", true);

    private JabRefExecutorService() {}

    @Override
    public void execute(Runnable command) {
        if(command == null) {
            //TODO logger
            return;
        }

        executorService.execute(command);
    }

    public void executeAndWait(Runnable command) {
        if(command == null) {
            //TODO logger
            return;
        }

        Future<?> future = executorService.submit(command);
        while(true) {
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

    private static class AutoCleanupRunnable implements Runnable {

        private final Runnable runnable;
        private final ConcurrentLinkedQueue<Thread> startedThreads;

        public Thread thread;

        private AutoCleanupRunnable(Runnable runnable, ConcurrentLinkedQueue<Thread> startedThreads) {
            this.runnable = runnable;
            this.startedThreads = startedThreads;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } finally {
                startedThreads.remove(thread);
            }
        }
    }

    public void executeInterruptableTask(final Runnable runnable) {
        this.lowPriorityExecutorService.execute(runnable);
    }

    public void executeInterruptableTaskAndWait(Runnable runnable) {
        if(runnable == null) {
            //TODO logger
            return;
        }

        Future<?> future = lowPriorityExecutorService.submit(runnable);
        while(true) {
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


    public void submit(TimerTask timerTask, long millisecondsDelay) {
        timer.schedule(timerTask, millisecondsDelay);
    }

    public void shutdownEverything() {
        // those threads will be allowed to finish
        this.executorService.shutdown();
        //those threads will be interrupted in their current task
        this.lowPriorityExecutorService.shutdownNow();
        // timer doesn't need to be canceled as it is run in daemon mode, which ensures that it is stopped if the application is shut down
    }

}
