package org.jabref.gui.logging;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.tinylog.core.LogEntry;

/**
 * This class is used for storing and archiving all message output of JabRef as log events.
 * To listen to changes on the stored logs one can bind to the {@code messagesProperty}.
 */
public class LogMessages {

    private static LogMessages instance = new LogMessages();
    
    // Maximum number of log messages to keep in memory
    private static final int MAX_MESSAGES = 10000;

    private final ObservableList<LogEntry> messages = FXCollections.observableArrayList();

    private LogMessages() {
    }

    public static LogMessages getInstance() {
        return instance;
    }

    public ObservableList<LogEntry> getMessages() {
        return FXCollections.unmodifiableObservableList(messages);
    }

    public void add(LogEntry event) {
        messages.add(event);
        
        // Keep only the last MAX_MESSAGES messages to prevent memory issues
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0, messages.size() - MAX_MESSAGES);
        }
    }

    public void clear() {
        messages.clear();
    }
}
