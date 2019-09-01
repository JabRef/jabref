package org.jabref.gui.preferences;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.layout.format.NameFormatter;

public class NameFormatterItemModel {
    private final StringProperty name;
    private final StringProperty format;

    NameFormatterItemModel() { this(""); }

    NameFormatterItemModel(String name) {
        this(name, NameFormatter.DEFAULT_FORMAT);
    }

    NameFormatterItemModel(String name, String format) {
        this.name = new SimpleStringProperty(name);
        this.format = new SimpleStringProperty(format);
    }

    public void setName(String name) {
        this.name.setValue(name);
    }

    public String getName() {
        return name.getValue();
    }

    public void setFormat(String format) {
        this.format.setValue(format);
    }

    public String getFormat() {
        return format.getValue();
    }

    @Override
    public String toString() { return "[" + name.getValue() + "," + format.getValue() + "]"; }
}
