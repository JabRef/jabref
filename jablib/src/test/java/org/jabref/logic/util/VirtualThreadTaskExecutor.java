package org.jabref.logic.util;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/// Runs tasks on virtual threads, as production does, so that asynchronous behaviour stays
/// observable in tests. Use [CurrentThreadTaskExecutor] where the test wants tasks to run inline.
public class VirtualThreadTaskExecutor implements TaskExecutor {

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(runnable -> Thread.ofVirtual().unstarted(runnable));

    @Override
    public <V> Future<V> execute(BackgroundTask<V> task) {
        return executor.submit(task::call);
    }

    @Override
    public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
        return executor.schedule(() -> execute(task), delay, unit);
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }

    @Override
    public DelayTaskThrottler createThrottler(int delay) {
        return new DelayTaskThrottler(delay);
    }
}
