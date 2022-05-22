package org.jabref.logic.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FetcherApiKey {
    private final StringProperty name;
    private final BooleanProperty use;
    private final StringProperty key;

    public FetcherApiKey(String name, boolean use, String key) {
        this.name = new SimpleStringProperty(name);
        this.use = new SimpleBooleanProperty(use);
        this.key = new SimpleStringProperty(key);
    }

    public String getName() {
        return name.getValue();
    }

    public boolean shouldUse() {
        return use.getValue();
    }

    public BooleanProperty useProperty() {
        return use;
    }

    public void setUse(boolean useCustom) {
        this.use.setValue(useCustom);
    }

    public String getKey() {
        return key.getValue();
    }

    public StringProperty keyProperty() {
        return key;
    }

    public void setKey(String key) {
        this.key.setValue(key);
    }
}
