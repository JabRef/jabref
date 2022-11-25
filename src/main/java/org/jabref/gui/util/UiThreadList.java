package org.jabref.gui.util;

import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UiThreadList<T> extends TransformationList<T, T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(UiThreadList.class);

    public UiThreadList(ObservableList<? extends T> source) {
        super(source);
    }

    @Override
    protected void sourceChanged(ListChangeListener.Change<? extends T> change) {
        if (Platform.isFxApplicationThread()) {
            fireChange(change);
        } else {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                fireChange(change);
                latch.countDown();
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                LOGGER.error("Error while running on JavaFX thread", e);
            }
        }
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
