package org.jabref.gui.strings;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


public class StringViewModel {

    private final StringProperty label;
    private final StringProperty content;

    public StringViewModel(String label, String content) {

        this.label = new SimpleStringProperty(label);
        this.content = new SimpleStringProperty(content);
    }

    public StringProperty getLabel() {
        return label;
    }

    public StringProperty getContent() {
        return content;
    }

    public void setLabel(String label) {
        this.label.setValue(label);
    }

    public void setContent(String content) {
        this.content.setValue(content);
    }
}
