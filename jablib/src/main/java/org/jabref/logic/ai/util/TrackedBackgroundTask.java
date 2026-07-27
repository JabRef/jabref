package org.jabref.logic.ai.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.ProgressCounter;

/// An extension to the [BackgroundTask] that stores the result (or exception) of the task inside.
///
/// Useful for the Ai features with task aggregators ([org.jabref.logic.ai.ingestion.IngestionTaskAggregator], [org.jabref.logic.ai.summarization.SummarizationTaskAggregator]),
/// because it allows to use the task object as a temporary storage.
public abstract class TrackedBackgroundTask<V> extends BackgroundTask<V> {
    public enum Status {
        PENDING,
        PROCESSING,
        SUCCESS,
        ERROR,
        CANCELLED;

        public boolean isFinished() {
            return this == SUCCESS || this == ERROR || this == CANCELLED;
        }
    }

    protected final ProgressCounter progressCounter = new ProgressCounter();

    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.PENDING);
    private final ObjectProperty<V> result = new SimpleObjectProperty<>();
    private final ObjectProperty<Exception> exception = new SimpleObjectProperty<>();

    public TrackedBackgroundTask() {
        super();
        progressCounter.listenToAllProperties(this::updateProgress);
    }

    public V call() throws Exception {
        try {
            status.set(Status.PROCESSING);

            V output = perform();

            if (status.get() == Status.CANCELLED) {
                return null;
            }

            result.set(output);
            status.set(Status.SUCCESS);

            return output;
        } catch (Exception e) {
            exception.set(e);
            status.set(Status.ERROR);
            throw e;
        }
    }

    public ReadOnlyObjectProperty<Status> statusProperty() {
        return status;
    }

    public Status getStatus() {
        return status.get();
    }

    public ReadOnlyObjectProperty<V> resultProperty() {
        return result;
    }

    public V getResult() {
        return result.get();
    }

    public ReadOnlyObjectProperty<Exception> exceptionProperty() {
        return exception;
    }

    public Exception getException() {
        return exception.get();
    }

    @Override
    public void cancel() {
        super.cancel();
        status.set(Status.CANCELLED);
    }

    private void updateProgress() {
        updateProgress(progressCounter.getWorkDone(), progressCounter.getWorkMax());
        updateMessage(progressCounter.getMessage());
    }

    protected abstract V perform() throws Exception;
}
