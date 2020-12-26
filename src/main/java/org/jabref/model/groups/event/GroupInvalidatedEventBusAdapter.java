package org.jabref.model.groups.event;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

import org.jabref.gui.util.DefaultTaskExecutor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Adapter class for the MetaData EventBus when dealing with changes and modifications of groups. Unlike the EventBus, that notifies all listeners on any changes, this adapter is intended to be used only for changes that require GUI updates.
 */
public class GroupInvalidatedEventBusAdapter extends EventBus implements Observable {
    private final Queue<InvalidationListener> listeners = new ConcurrentLinkedQueue<>();

    public GroupInvalidatedEventBusAdapter() {
        super();
        this.register(this);
    }

    @Subscribe
    public void listen(GroupInvalidatedEvent groupInvalidatedEvent) {
        notifyAllListeners();
    }

    private void notifyAllListeners() {
        DefaultTaskExecutor.runInJavaFXThread(() -> listeners.forEach(listener -> listener.invalidated(this)));
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listeners.remove(listener);
    }
}
