package org.jabref.gui.util.uithreadaware;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableStringValue;

/// This class can be used to wrap a @see StringProperty inside it. When wrapped, any Listener listening for updates to the wrapped StringProperty (for example because of a binding to it) is ensured to be notified on the JavaFX Application Thread. It should be used to implement bindings where updates come in from a background thread but should be reflected in the UI where it is necessary that changes to the UI are performed on the JavaFX Application thread.
///
/// Accepts anything shaped like a `StringProperty` (bindable + a writable String value), not just the
/// concrete `StringProperty` class — e.g. jfxcore's `ConstrainedStringProperty`.
public class UiThreadStringProperty extends StringProperty {

    private final Property<String> delegate;
    private final WritableStringValue writableDelegate;

    public <T extends Property<String> & WritableStringValue> UiThreadStringProperty(T delegate) {
        this.delegate = delegate;
        this.writableDelegate = delegate;
    }

    @Override
    public void bind(ObservableValue<? extends String> observable) {
        delegate.bind(observable);
    }

    @Override
    public void unbind() {
        delegate.unbind();
    }

    @Override
    public boolean isBound() {
        return delegate.isBound();
    }

    @Override
    public Object getBean() {
        return delegate.getBean();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String get() {
        return writableDelegate.get();
    }

    @Override
    public void set(String value) {
        writableDelegate.set(value);
    }

    @Override
    public void addListener(ChangeListener<? super String> listener) {
        delegate.addListener(new UiThreadChangeListener<>(listener));
    }

    @Override
    public void removeListener(ChangeListener<? super String> listener) {
        delegate.removeListener(listener);
    }

    @Override
    public void addListener(InvalidationListener listener) {
        delegate.addListener(new UiThreadInvalidationListener(listener));
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        delegate.removeListener(listener);
    }
}
