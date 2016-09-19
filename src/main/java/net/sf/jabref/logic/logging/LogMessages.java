package net.sf.jabref.logic.logging;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.apache.logging.log4j.core.LogEvent;

/**
 * This class is used for storing and archiving all message output of JabRef as log events.
 * To listen to changes on the stored logs one can bind to the {@code messagesProperty}.
 */
public class LogMessages {

    private static LogMessages instance = new LogMessages();

    private LogMessages() {
    }

    public static LogMessages getInstance() {
        return instance;
    }

    private final ListProperty<LogEvent> messages = new SimpleListProperty<>(FXCollections.observableArrayList());

    public ListProperty<LogEvent> messagesProperty() {
        return messages;
    }

    public void add(LogEvent s) {
        messages.add(s);
    }

}
