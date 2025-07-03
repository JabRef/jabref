package org.jabref.http.server.cayw.gui;

import java.util.Objects;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.model.entry.BibEntry;

public class CAYWEntry {

    private final BibEntry value;

    // Used on the buttons ("chips")
    private final String shortLabel;

    // Used in the list
    private final String label;

    // Used when hovering and used as bases on the second line
    private final String description;

    private EventHandler<ActionEvent> onClick;

    public CAYWEntry(BibEntry value, String label, String shortLabel, String description) {
        this.value = value;
        this.label = label;
        this.shortLabel = shortLabel;
        this.description = description;
    }

    public BibEntry getValue() {
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CAYWEntry caywEntry = (CAYWEntry) o;
        return Objects.equals(getValue(), caywEntry.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }
}
