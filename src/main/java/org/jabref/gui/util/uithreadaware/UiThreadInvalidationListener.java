package org.jabref.gui.util.uithreadaware;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

class UiThreadInvalidationListener implements InvalidationListener {

    private final InvalidationListener delegate;

    public UiThreadInvalidationListener(InvalidationListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void invalidated(Observable observable) {
        UiThreadHelper.ensureUiThreadExecution(() -> delegate.invalidated(observable));
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
