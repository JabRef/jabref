package org.jabref.logic.ai.processingstatus;

import javafx.beans.property.ObjectProperty;

import jakarta.annotation.Nullable;

public record ProcessingInfo<T, D>(
        T object,
        ObjectProperty<ProcessingState> state,
        ObjectProperty<Exception> exception,
        ObjectProperty<D> data
) {
    public void setSuccess(@Nullable D data) {
        // Listeners will probably handle only state property, so be careful to set the data BEFORE setting the state.
        data().set(data);
        state().set(ProcessingState.SUCCESS);
    }

    public void setException(Exception exception) {
        // Listeners will probably handle only state property, so be careful to set the error message BEFORE setting the state.
        exception().set(exception);
        state().set(ProcessingState.ERROR);
    }
}
