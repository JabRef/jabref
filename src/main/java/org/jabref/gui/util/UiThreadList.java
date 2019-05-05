package org.jabref.gui.util;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;

class UiThreadList<T> extends TransformationList<T, T> {
    public UiThreadList(ObservableList<? extends T> source) {
        super(source);
    }

    @Override
    protected void sourceChanged(ListChangeListener.Change<? extends T> change) {
        Platform.runLater(() -> fireChange(change));
    }

    @Override
    public int getSourceIndex(int index) {
        return index;
    }

    @Override
    public int getViewIndex(int index) {
        return index;
    }

    @Override
    public T get(int index) {
        return getSource().get(index);
    }

    @Override
    public int size() {
        return getSource().size();
    }
}
