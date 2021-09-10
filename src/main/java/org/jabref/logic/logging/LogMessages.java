package org.jabref.logic.logging;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.event.LoggingEvent;

/**
 * This class is used for storing and archiving all message output of JabRef as log events.
 * To listen to changes on the stored logs one can bind to the {@code messagesProperty}.
 */
public class LogMessages {

    private static LogMessages instance = new LogMessages();

    private final ObservableList<LoggingEvent> messages = FXCollections.observableArrayList();

    private LogMessages() {
    }

    public static LogMessages getInstance() {
        return instance;
    }

    public ObservableList<LoggingEvent> getMessages() {
        return FXCollections.unmodifiableObservableList(messages);
    }

    public void add(LoggingEvent event) {
        messages.add(event);
    }

    public void clear() {
        messages.clear();
    }
}
