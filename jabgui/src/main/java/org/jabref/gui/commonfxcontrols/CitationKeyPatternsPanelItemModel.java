package org.jabref.gui.commonfxcontrols;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.entry.types.EntryType;

import org.jspecify.annotations.NonNull;

public class CitationKeyPatternsPanelItemModel {
    private final ObjectProperty<EntryType> entryType = new SimpleObjectProperty<>();
    private final StringProperty pattern = new SimpleStringProperty("");

    public CitationKeyPatternsPanelItemModel(@NonNull EntryType entryType, @NonNull String pattern) {
        this.entryType.setValue(entryType);
        this.pattern.setValue(pattern);
    }

    public EntryType getEntryType() {
        return entryType.getValue();
    }

    public ObjectProperty<EntryType> entryType() {
        return entryType;
    }

    public void setPattern(String pattern) {
        this.pattern.setValue(pattern);
    }

    public String getPattern() {
        return pattern.getValue();
    }

    public StringProperty pattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return "[" + entryType.getValue().getName() + "," + pattern.getValue() + "]";
    }
}
