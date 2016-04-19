/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import net.sf.jabref.gui.undo.UndoableInsertEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Responsible for managing of all threads (except Swing threads) in JabRef
 */
public class JabRefExecutorService implements Executor {

    private static final Log LOGGER = LogFactory.getLog(UndoableInsertEntry.class);

    public static final JabRefExecutorService INSTANCE = new JabRefExecutorService();

    private final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("JabRef CachedThreadPool");
            return thread;
        }
    });
    private final ConcurrentLinkedQueue<Thread> startedThreads = new ConcurrentLinkedQueue<>();

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
            } catch (InterruptedException ignored) {
                // Ignored
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
