package net.sf.jabref.logic.logging;

import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import net.sf.jabref.logic.error.LogMessageWithPriority;

/**
 * This LogMessage will be save all message entries in a ListProperty.
 * <p></p>
 * This list will be use late in ErrorConsoleViewModel to filter message after their priority
 */
public class LogMessage {

    private static LogMessage instance = new LogMessage();

    private LogMessage() {
    }

    public static LogMessage getInstance() {
        return instance;
    }

    private final ListProperty<LogMessageWithPriority> messages = new SimpleListProperty<>(FXCollections.observableArrayList((item ->
            new Observable[]{item.isFilteredProperty()})));

    public ListProperty<LogMessageWithPriority> messagesProperty() {
        return messages;
    }

    public void add(LogMessageWithPriority s) {
        messages.add(s);
    }

}
