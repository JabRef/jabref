package org.jabref.http.server.cayw.gui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class CAYWEntry<T> {

    private final T value;
    private final String label;
    private final String shortLabel;
    private final String description;

    private EventHandler<ActionEvent> onClick;

    public CAYWEntry(T value, String label, String shortLabel, String description) {
        this.value = value;
        this.label = label;
        this.shortLabel = shortLabel;
        this.description = description;
    }

    public T getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getDescription() {
        return description;
    }

    public EventHandler<ActionEvent> getOnClick() {
        return onClick;
    }

    public void setOnClick(EventHandler<ActionEvent> onClick) {
        this.onClick = onClick;
    }
}
