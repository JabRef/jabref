package org.jabref.gui.util.uithreadaware;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

import com.tobiasdiez.easybind.EasyBinding;
import com.tobiasdiez.easybind.optional.OptionalBinding;

/**
 * This class can be used to wrap a {@link OptionalBinding} inside it. When wrapped, any Listener listening for updates to the wrapped {@link OptionalBinding} (for example because of a binding to it) is ensured to be notified on the JavaFX Application Thread. It should be used to implement bindings where updates come in from a background thread but should be reflected in the UI where it is necessary that changes to the UI are performed on the JavaFX Application thread.
 */
public class UiThreadOptionalBinding<T> implements OptionalBinding<T> {

    private final OptionalBinding<T> delegate;

    public UiThreadOptionalBinding(OptionalBinding<T> delegate) {
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
    public void addListener(ChangeListener<? super Optional<T>> listener) {
        delegate.addListener(new UiThreadChangeListener<>(listener));
    }

    @Override
    public void removeListener(ChangeListener<? super Optional<T>> listener) {
        delegate.removeListener(listener);
    }

    @Override
    public Optional<T> getValue() {
        return delegate.getValue();
    }

    @Override
    public EasyBinding<T> orElse(T other) {
        return delegate.orElse(other);
    }

    @Override
    public OptionalBinding<T> orElse(ObservableValue<T> other) {
        return delegate.orElse(other);
    }

    @Override
    public OptionalBinding<T> filter(Predicate<? super T> predicate) {
        return delegate.filter(predicate);
    }

    @Override
    public <U> OptionalBinding<U> map(Function<? super T, ? extends U> mapper) {
        return delegate.map(mapper);
    }

    @Override
    public <U> OptionalBinding<U> flatMap(Function<T, Optional<U>> mapper) {
        return delegate.flatMap(mapper);
    }

    @Override
    public BooleanBinding isPresent() {
        return delegate.isPresent();
    }

    @Override
    public BooleanBinding isEmpty() {
        return delegate.isEmpty();
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

    @Override
    public Optional<T> get() {
        return delegate.get();
    }
}
