package org.jabref.gui.util;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FieldColumnCellValues {

    private final StringProperty cellText = new SimpleStringProperty();
    private final StringProperty tooltipText = new SimpleStringProperty();

    public FieldColumnCellValues(String cellText, String tooltipText) {
        this.cellText.set(cellText);
        this.tooltipText.set(tooltipText);
    }

    public StringProperty cellTextProperty() {
        return cellText;
    }

    public StringProperty tooltipTextProperty() {
        return tooltipText;
    }
}
