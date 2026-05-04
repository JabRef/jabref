package org.jabref.logic.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class OwnerPreferences {
    private static final String DEFAULT_OWNER = System.getProperty("user.name");

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

    private OwnerPreferences() {
        this(
                false,         // useOwner
                DEFAULT_OWNER, // defaultOwner
                false          // overwriteOwner
        );
    }

    public static OwnerPreferences getDefault() {
        return new OwnerPreferences();
    }

    public OwnerPreferences setAll(OwnerPreferences preferences) {
        setUseOwner(preferences.shouldUseOwner());
        setDefaultOwner(preferences.getDefaultOwner());
        setOverwriteOwner(preferences.shouldOverwriteOwner());
        return this;
    }

    public boolean shouldUseOwner() {
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

    public boolean shouldOverwriteOwner() {
        return overwriteOwner.getValue();
    }

    public BooleanProperty overwriteOwnerProperty() {
        return overwriteOwner;
    }

    public void setOverwriteOwner(boolean overwriteOwner) {
        this.overwriteOwner.set(overwriteOwner);
    }
}
