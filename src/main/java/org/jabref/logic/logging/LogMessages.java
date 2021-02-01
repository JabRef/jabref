package org.jabref.logic.logging;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.apache.logging.log4j.core.LogEvent;

/**
 * This class is used for storing and archiving all message output of JabRef as log events.
 * To listen to changes on the stored logs one can bind to the {@code messagesProperty}.
 */
public class LogMessages {

    private static LogMessages instance = new LogMessages();

    private final ObservableList<LogEvent> messages = FXCollections.observableArrayList();

    private LogMessages() {
    }

    public static LogMessages getInstance() {
        return instance;
    }

    public ObservableList<LogEvent> getMessages() {
        return FXCollections.unmodifiableObservableList(messages);
    }

    public void add(LogEvent event) {
        messages.add(event);
    }

    public void clear() {
        messages.clear();
    }
}
