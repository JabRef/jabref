package org.jabref.gui.util;

import java.util.function.Consumer;

import org.jabref.gui.externalfiles.FileDownloadTask;

import com.jcabi.log.Logger;

/**
 * Implementation of {@link TaskExecutor} that runs every task on the current thread, i.e. in a sequential order.
 * This class is not designed to be used in production but should make code involving asynchronous operations
 * deterministic and testable.
 */
public class CurrentThreadTaskExecutor implements TaskExecutor {


    /**
     * Executes the task on the current thread.
     * The code is essentially taken from {@link javafx.concurrent.Task.TaskCallable#call()},
     * but adapted to run sequentially.
     */
    @Override
    public <V> void execute(BackgroundTask<V> task) {
        Runnable onRunning = task.getOnRunning();
        if (onRunning != null) {
            onRunning.run();
        }
        try {
            final V result = task.call();
            Consumer<V> onSuccess = task.getOnSuccess();
            if (onSuccess != null) {
                onSuccess.accept(result);
            }
        } catch (Exception exception) {
            Consumer<Exception> onException = task.getOnException();
            if (onException != null) {
                onException.accept(exception);
            } else {
                Logger.error(this, "Unhandled exception: %[exception]s", exception);
            }
        }
    }

    @Override
    public void execute(FileDownloadTask downloadTask) {
        downloadTask.run();
    }

    @Override
    public void shutdown() {
        // Nothing to do here
    }
}
