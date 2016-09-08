package net.sf.jabref.logic.logging;

import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import net.sf.jabref.logic.error.LogMessage;

/**
 * This call saves all messages in a list.
 * <p></p>
 * The list is used in ErrorConsoleViewModel to filter messages according to their priority.
 */
public class LogMessages {

    private static LogMessages instance = new LogMessages();

    private LogMessages() {
    }

    public static LogMessages getInstance() {
        return instance;
    }

    private final ListProperty<LogMessage> messages = new SimpleListProperty<>(FXCollections.observableArrayList((item ->
            new Observable[]{item.isFilteredProperty()})));

    public ListProperty<LogMessage> messagesProperty() {
        return messages;
    }

    public void add(LogMessage s) {
        messages.add(s);
    }

}
