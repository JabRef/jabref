package org.jabref.gui.util.uithreadaware;

import javafx.collections.ListChangeListener;

class UiThreadListChangeListener<E> implements ListChangeListener<E> {

    private final ListChangeListener<E> delegate;

    public UiThreadListChangeListener(ListChangeListener<E> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onChanged(Change<? extends E> c) {
        UiThreadHelper.ensureUiThreadExecution(() -> delegate.onChanged(c));
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
