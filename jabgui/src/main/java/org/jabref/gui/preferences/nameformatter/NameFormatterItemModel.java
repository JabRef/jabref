package org.jabref.gui.preferences.nameformatter;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.layout.format.NameFormatter;

public class NameFormatterItemModel {
    private final StringProperty name = new SimpleStringProperty("");
    private final StringProperty format = new SimpleStringProperty("");

    NameFormatterItemModel() {
        this("");
    }

    NameFormatterItemModel(String name) {
        this(name, NameFormatter.DEFAULT_FORMAT);
    }

    NameFormatterItemModel(String name, String format) {
        this.name.setValue(name);
        this.format.setValue(format);
    }

    public void setName(String name) {
        this.name.setValue(name);
    }

    public String getName() {
        return name.getValue();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setFormat(String format) {
        this.format.setValue(format);
    }

    public String getFormat() {
        return format.getValue();
    }

    public StringProperty formatProperty() {
        return format;
    }

    @Override
    public String toString() {
        return "[" + name.getValue() + "," + format.getValue() + "]";
    }
}
