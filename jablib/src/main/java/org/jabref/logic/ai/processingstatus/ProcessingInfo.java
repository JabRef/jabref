package org.jabref.logic.ai.processingstatus;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import jakarta.annotation.Nullable;

public class ProcessingInfo<O, D> {
    private final O object;
    private final ObjectProperty<ProcessingState> state;
    private Optional<Exception> exception = Optional.empty();
    private Optional<D> data = Optional.empty();

    public ProcessingInfo(O object, ProcessingState state) {
        this.object = object;
        this.state = new SimpleObjectProperty<>(state);
    }

    public void setSuccess(@Nullable D data) {
        // Listeners will probably handle only state property, so be careful to set the data BEFORE setting the state.
        this.data = Optional.ofNullable(data);
        this.state.set(ProcessingState.SUCCESS);
    }

    public void setException(Exception exception) {
        // Listeners will probably handle only state property, so be careful to set the error message BEFORE setting the state.
        this.exception = Optional.of(exception);
        this.state.set(ProcessingState.ERROR);
    }

    public O getObject() {
        return object;
    }

    public ProcessingState getState() {
        return state.get();
    }

    public void setState(ProcessingState state) {
        this.state.set(state);
    }

    public ReadOnlyObjectProperty<ProcessingState> stateProperty() {
        return state;
    }

    public Optional<Exception> getException() {
        return exception;
    }

    public Optional<D> getData() {
        return data;
    }
}
