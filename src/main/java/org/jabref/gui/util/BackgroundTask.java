package org.jabref.gui.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.Node;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

import com.google.common.collect.ImmutableMap;
import com.tobiasdiez.easybind.EasyBind;

/**
 * This class is essentially a wrapper around {@link Task}.
 * We cannot use {@link Task} directly since it runs certain update notifications on the JavaFX thread,
 * and so makes testing harder.
 * We take the opportunity and implement a fluid interface.
 *
 * @param <V> type of the return value of the task
 */
public abstract class BackgroundTask<V> {

    public static ImmutableMap<String, Node> iconMap = ImmutableMap.of(
            Localization.lang("Downloading"), IconTheme.JabRefIcons.DOWNLOAD.getGraphicNode()
    );

    private Runnable onRunning;
    private Consumer<V> onSuccess;
    private Consumer<Exception> onException;
    private Runnable onFinished;
    private final BooleanProperty isCanceled = new SimpleBooleanProperty(false);
    private final ObjectProperty<BackgroundProgress> progress = new SimpleObjectProperty<>(new BackgroundProgress(0, 0));
    private final StringProperty message = new SimpleStringProperty("");
    private final StringProperty title = new SimpleStringProperty(this.getClass().getSimpleName());
    private final DoubleProperty workDonePercentage = new SimpleDoubleProperty(0);
    private final BooleanProperty showToUser = new SimpleBooleanProperty(false);

    public BackgroundTask() {
        workDonePercentage.bind(EasyBind.map(progress, BackgroundTask.BackgroundProgress::getWorkDonePercentage));
    }

    public static <V> BackgroundTask<V> wrap(Callable<V> callable) {
        return new BackgroundTask<>() {
            @Override
            protected V call() throws Exception {
                return callable.call();
            }
        };
    }

    public static BackgroundTask<Void> wrap(Runnable runnable) {
        return new BackgroundTask<>() {
            @Override
            protected Void call() {
                runnable.run();
                return null;
            }
        };
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

    public boolean isCanceled() {
        return isCanceled.get();
    }

    public void cancel() {
        this.isCanceled.set(true);
    }

    public BooleanProperty isCanceledProperty() {
        return isCanceled;
    }

    public StringProperty messageProperty() {
        return message;
    }

    public StringProperty titleProperty() {
        return title;
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

    public boolean showToUser() {
        return showToUser.get();
    }

    public void showToUser(boolean show) {
        showToUser.set(show);
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
     * The consumer always runs on the JavaFX thread.
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

    /**
     * Sets the {@link Consumer} that is invoked after the task has failed with an exception.
     * The consumer always runs on the JavaFX thread.
     */
    public BackgroundTask<V> onFailure(Consumer<Exception> onException) {
        this.onException = onException;
        return this;
    }

    public Future<?> executeWith(TaskExecutor taskExecutor) {
        return taskExecutor.execute(this);
    }

    public Future<?> scheduleWith(TaskExecutor taskExecutor, long delay, TimeUnit unit) {
        return taskExecutor.schedule(this, delay, unit);
    }

    /**
     * Sets the {@link Runnable} that is invoked after the task is finished, irrespectively if it was successful or
     * failed with an error.
     */
    public BackgroundTask<V> onFinished(Runnable onFinished) {
        this.onFinished = onFinished;
        return this;
    }

    /**
     * Creates a {@link BackgroundTask} that first runs this task and based on the result runs a second task.
     *
     * @param nextTaskFactory the function that creates the new task
     * @param <T>             type of the return value of the second task
     */
    public <T> BackgroundTask<T> then(Function<V, BackgroundTask<T>> nextTaskFactory) {
        return new BackgroundTask<>() {
            @Override
            protected T call() throws Exception {
                V result = BackgroundTask.this.call();
                BackgroundTask<T> nextTask = nextTaskFactory.apply(result);
                EasyBind.subscribe(nextTask.progressProperty(), this::updateProgress);
                return nextTask.call();
            }
        };
    }

    /**
     * Creates a {@link BackgroundTask} that first runs this task and based on the result runs a second task.
     *
     * @param nextOperation the function that performs the next operation
     * @param <T>           type of the return value of the second task
     */
    public <T> BackgroundTask<T> thenRun(Function<V, T> nextOperation) {
        return new BackgroundTask<>() {
            @Override
            protected T call() throws Exception {
                V result = BackgroundTask.this.call();
                BackgroundTask<T> nextTask = BackgroundTask.wrap(() -> nextOperation.apply(result));
                EasyBind.subscribe(nextTask.progressProperty(), this::updateProgress);
                return nextTask.call();
            }
        };
    }

    /**
     * Creates a {@link BackgroundTask} that first runs this task and based on the result runs a second task.
     *
     * @param nextOperation the function that performs the next operation
     */
    public BackgroundTask<Void> thenRun(Consumer<V> nextOperation) {
        return new BackgroundTask<>() {
            @Override
            protected Void call() throws Exception {
                V result = BackgroundTask.this.call();
                BackgroundTask<Void> nextTask = BackgroundTask.wrap(() -> nextOperation.accept(result));
                EasyBind.subscribe(nextTask.progressProperty(), this::updateProgress);
                return nextTask.call();
            }
        };
    }

    protected void updateProgress(BackgroundProgress newProgress) {
        progress.setValue(newProgress);
    }

    protected void updateProgress(double workDone, double max) {
        updateProgress(new BackgroundProgress(workDone, max));
    }

    protected void updateMessage(String newMessage) {
        message.setValue(newMessage);
    }

    public BackgroundTask<V> withInitialMessage(String message) {
        updateMessage(message);
        return this;
    }

    public static Node getIcon(Task<?> task) {
        return BackgroundTask.iconMap.getOrDefault(task.getTitle(), null);
    }

    static class BackgroundProgress {

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
