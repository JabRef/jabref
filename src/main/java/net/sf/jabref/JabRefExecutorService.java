package net.sf.jabref;

import java.util.concurrent.*;

/**
 * Responsible for managing of all threads (except Swing threads) in JabRef
 */
public class JabRefExecutorService implements Executor {

    public static final JabRefExecutorService INSTANCE = new JabRefExecutorService();

    private final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("JabRef CachedThreadPool");
            return thread;
        }
    });
    private final ConcurrentLinkedQueue<Thread> startedThreads = new ConcurrentLinkedQueue<Thread>();

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
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
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

    public void executeWithLowPriorityInOwnThread(final Runnable runnable, String name) {
        AutoCleanupRunnable target = new AutoCleanupRunnable(runnable, startedThreads);
        final Thread thread = new Thread(target);
        target.thread = thread;
        thread.setName("JabRef - " + name + " - low prio");
        startedThreads.add(thread);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public void executeInOwnThread(Thread thread) {
        // this is a special case method for Threads that cannot be interrupted so easily
        // this method should normally not be used
        startedThreads.add(thread);
        // TODO memory leak when thread is finished
        thread.start();
    }

    public void executeWithLowPriorityInOwnThreadAndWait(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName("JabRef low prio");
        startedThreads.add(thread);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();

        waitForThreadToFinish(thread);
    }

    private void waitForThreadToFinish(Thread thread) {
        while(true) {
            try {
                thread.join();
                startedThreads.remove(thread);
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdownEverything() {
        this.executorService.shutdown();
        for(Thread thread : startedThreads) {
            thread.interrupt();
        }
        startedThreads.clear();
    }

}
