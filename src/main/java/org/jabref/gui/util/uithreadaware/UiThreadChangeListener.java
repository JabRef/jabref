package org.jabref.gui.util.uithreadaware;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

class UiThreadChangeListener<T> implements ChangeListener<T> {

    private ChangeListener<T> delegate;

    public UiThreadChangeListener(ChangeListener<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
        UiThreadHelper.ensureUiThreadExecution(() -> delegate.changed(observable, oldValue, newValue));
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
