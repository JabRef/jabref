package org.jabref.logic.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class OwnerPreferences {
    private final BooleanProperty useOwner;
    private final StringProperty defaultOwner;
    private final BooleanProperty overwriteOwner;

    public OwnerPreferences(boolean useOwner,
                            String defaultOwner,
                            boolean overwriteOwner) {
        this.useOwner = new SimpleBooleanProperty(useOwner);
        this.defaultOwner = new SimpleStringProperty(defaultOwner);
        this.overwriteOwner = new SimpleBooleanProperty(overwriteOwner);
    }

    public boolean isUseOwner() {
        return useOwner.getValue();
    }

    public BooleanProperty useOwnerProperty() {
        return useOwner;
    }

    public void setUseOwner(boolean useOwner) {
        this.useOwner.set(useOwner);
    }

    public String getDefaultOwner() {
        return defaultOwner.getValue();
    }

    public StringProperty defaultOwnerProperty() {
        return defaultOwner;
    }

    public void setDefaultOwner(String defaultOwner) {
        this.defaultOwner.set(defaultOwner);
    }

    public boolean isOverwriteOwner() {
        return overwriteOwner.getValue();
    }

    public BooleanProperty overwriteOwnerProperty() {
        return overwriteOwner;
    }

    public void setOverwriteOwner(boolean overwriteOwner) {
        this.overwriteOwner.set(overwriteOwner);
    }
}
