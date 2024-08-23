package org.jabref.logic.ai.processingstatus;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;

import jakarta.annotation.Nullable;

public record ProcessingInfo<T, D>(
        T object,
        ObjectProperty<ProcessingState> state,
        StringProperty errorMessage,
        ObjectProperty<D> data
) {
    public void setSuccess(@Nullable D data) {
        // Listeners will probably handle only state property, so be careful to set the data BEFORE setting the state.
        data().set(data);
        state().set(ProcessingState.SUCCESS);
    }

    public void setError(String errorMessage) {
        // Listeners will probably handle only state property, so be careful to set the error message BEFORE setting the state.
        errorMessage().set(errorMessage);
        state().set(ProcessingState.ERROR);
    }

    public void setError(Exception e) {
        setError(e.getMessage());
    }
}
