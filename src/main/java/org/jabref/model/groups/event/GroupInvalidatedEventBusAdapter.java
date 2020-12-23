package org.jabref.model.groups.event;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

import org.jabref.gui.util.DefaultTaskExecutor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

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

    @Subscribe
    public void listen(GroupUpdatedEvent groupUpdatedEvent) {
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
