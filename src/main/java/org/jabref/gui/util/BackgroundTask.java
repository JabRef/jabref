package org.jabref.gui.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;

import org.fxmisc.easybind.EasyBind;

/**
 * This class is essentially a wrapper around {@link Task}.
 * We cannot use {@link Task} directly since it runs certain update notifications on the JavaFX thread,
 * and so makes testing harder.
 * We take the opportunity and implement a fluid interface.
 *
 * @param <V> type of the return value of the task
 */
public abstract class BackgroundTask<V> {
    private Runnable onRunning;
    private Consumer<V> onSuccess;
    private Consumer<Exception> onException;
    private Runnable onFinished;
    private ObjectProperty<BackgroundProgress> progress = new SimpleObjectProperty<>(new BackgroundProgress(0, 0));
    private DoubleProperty workDonePercentage = new SimpleDoubleProperty(0);

    public BackgroundTask() {
        workDonePercentage.bind(EasyBind.map(progress, BackgroundTask.BackgroundProgress::getWorkDonePercentage));
    }

    public static <V> BackgroundTask<V> wrap(Callable<V> callable) {
        return new BackgroundTask<V>() {
            @Override
            protected V call() throws Exception {
                return callable.call();
            }
        };
    }

    public double getWorkDonePercentage() {
        return workDonePercentage.get();
    }

    public DoubleProperty workDonePercentageProperty() {
        return workDonePercentage;
    }

    public BackgroundProgress getProgress() {
        return progress.get();
    }

    public ObjectProperty<BackgroundProgress> progressProperty() {
        return progress;
    }

    private static <T> Consumer<T> chain(Runnable first, Consumer<T> second) {
        if (first != null) {
            if (second != null) {
                return result -> {
                    first.run();
                    second.accept(result);
                };
            } else {
                return result -> first.run();
            }
        } else {
            return second;
        }
    }

    /**
     * Sets the {@link Runnable} that is invoked after the task is started.
     */
    public BackgroundTask<V> onRunning(Runnable onRunning) {
        this.onRunning = onRunning;
        return this;
    }

    /**
     * Sets the {@link Consumer} that is invoked after the task is successfully finished.
     */
    public BackgroundTask<V> onSuccess(Consumer<V> onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    protected abstract V call() throws Exception;

    Runnable getOnRunning() {
        return onRunning;
    }

    Consumer<V> getOnSuccess() {
        return chain(onFinished, onSuccess);
    }

    Consumer<Exception> getOnException() {
        return chain(onFinished, onException);
    }

    public BackgroundTask<V> onFailure(Consumer<Exception> onException) {
        this.onException = onException;
        return this;
    }

    public Future<?> executeWith(TaskExecutor taskExecutor) {
        return taskExecutor.execute(this);
    }

    /**
     * Sets the {@link Runnable} that is invoked after the task is finished, irrespectively if it was successful or
     * failed with an error.
     */
    public BackgroundTask<V> onFinished(Runnable onFinished) {
        this.onFinished = onFinished;
        return this;
    }

    protected void updateProgress(double workDone, double max) {
        progress.setValue(new BackgroundProgress(workDone, max));
    }

    public class BackgroundProgress {

        private final double workDone;
        private final double max;

        public BackgroundProgress(double workDone, double max) {
            this.workDone = workDone;
            this.max = max;
        }

        public double getWorkDone() {
            return workDone;
        }

        public double getMax() {
            return max;
        }

        public double getWorkDonePercentage() {
            if (max == 0) {
                return 0;
            } else {
                return workDone / max;
            }
        }
    }
}
