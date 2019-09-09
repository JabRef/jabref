package org.jabref.gui.util.uithreadaware;

import javafx.beans.InvalidationListener;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class UiThreadStringProperty extends StringProperty {

    private final StringProperty delegate;

    public UiThreadStringProperty(StringProperty delegate) {
        this.delegate = delegate;
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
        return delegate.get();
    }

    @Override
    public void set(String value) {
        UiThreadHelper.ensureUiThreadExecution(() -> delegate.set(value));
    }

    @Override
    public void addListener(ChangeListener<? super String> listener) {
        delegate.addListener(new UiThreadChangeListener(listener));
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
