package org.jabref.gui.util.uithreadaware;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Binding;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;

/**
 * This class can be used to wrap a {@link Binding} inside it. When wrapped, any Listener listening for updates to the wrapped {@link Binding} (for example because of a binding to it) is ensured to be notified on the JavaFX Application Thread. It should be used to implement bindings where updates come in from a background thread but should be reflected in the UI where it is necessary that changes to the UI are performed on the JavaFX Application thread.
 */
public class UiThreadBinding<T> implements Binding<T> {

    private final Binding<T> delegate;

    public UiThreadBinding(Binding<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        delegate.addListener(new UiThreadInvalidationListener(listener));
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        delegate.removeListener(listener);
    }

    @Override
    public void addListener(ChangeListener<? super T> listener) {
        delegate.addListener(new UiThreadChangeListener<>(listener));
    }

    @Override
    public void removeListener(ChangeListener<? super T> listener) {
        delegate.removeListener(listener);
    }

    @Override
    public T getValue() {
        return delegate.getValue();
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    public void invalidate() {
        delegate.invalidate();
    }

    @Override
    public ObservableList<?> getDependencies() {
        return delegate.getDependencies();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }
}
