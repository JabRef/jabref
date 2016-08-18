package net.sf.jabref.logic.logging;

import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import net.sf.jabref.logic.error.ObservableMessageWithPriority;

/**
 * This ObservableMessages enum will be save all message entries in a ListProperty.
 * <p></p>
 * This list will be use late in ErrorConsoleViewModel class to filter message after their priority
 */
public enum ObservableMessages {

    INSTANCE;

    private final ListProperty<ObservableMessageWithPriority> messages = new SimpleListProperty<>(FXCollections.observableArrayList((item ->
            new Observable[]{item.isFilteredProperty()})));

    public ListProperty<ObservableMessageWithPriority> messagesPropety() {
        return messages;
    }

    public void add(ObservableMessageWithPriority s) {
        messages.add(s);
    }

}
