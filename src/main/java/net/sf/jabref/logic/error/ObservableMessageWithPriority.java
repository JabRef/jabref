package net.sf.jabref.logic.error;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ObservableMessageWithPriority {

    private String message;

    private MessagePriority priority;

    private BooleanProperty isFiltered = new SimpleBooleanProperty();

    public ObservableMessageWithPriority(String message, MessagePriority priority) {
        this.message = message;
        this.priority = priority;
        isFiltered.set(priority != MessagePriority.LOW);
    }

    public MessagePriority getPriority() {
        return priority;
    }

    public void setPriority(MessagePriority priority) {
        this.priority = priority;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getIsFiltered() {
        return isFiltered.get();
    }

    public void setIsFiltered(boolean isFiltered) {
        this.isFiltered.set(isFiltered);
    }

    public BooleanProperty isFilteredProperty() {
        return isFiltered;
    }

}
