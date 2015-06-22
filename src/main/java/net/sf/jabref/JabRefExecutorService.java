package net.sf.jabref;

import java.util.concurrent.*;

/**
 * Responsible for managing of all threads (except Swing threads) in JabRef
 */
public class JabRefExecutorService implements Executor {

    public static JabRefExecutorService INSTANCE = new JabRefExecutorService();

    private final ExecutorService executorService = Executors.newCachedThreadPool();

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

    public void executeWithLowPriorityInOwnThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    public void executeInOwnThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
    }

    public void executeWithLowPriorityInOwnThreadAndWait(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();

        while(true) {
            try {
                thread.join();
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
