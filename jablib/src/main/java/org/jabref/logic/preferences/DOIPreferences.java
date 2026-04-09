package org.jabref.logic.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DOIPreferences {
    private final BooleanProperty useCustom;
    private final StringProperty defaultBaseURI;

    public DOIPreferences(boolean useCustom, String defaultBaseURI) {
        this.useCustom = new SimpleBooleanProperty(useCustom);
        this.defaultBaseURI = new SimpleStringProperty(defaultBaseURI);
    }

    private DOIPreferences() {
        this(
                false,            // useCustom
                "https://doi.org" // defaultBaseURI
        );
    }

    public static DOIPreferences getDefault() {
        return new DOIPreferences();
    }

    public void setAll(DOIPreferences preferences) {
        this.useCustom.set(preferences.shouldUseCustom());
        this.defaultBaseURI.set(preferences.getDefaultBaseURI());
    }

    public boolean shouldUseCustom() {
        return useCustom.get();
    }

    public BooleanProperty useCustomProperty() {
        return useCustom;
    }

    public void setUseCustom(boolean useCustom) {
        this.useCustom.set(useCustom);
    }

    public String getDefaultBaseURI() {
        return defaultBaseURI.get();
    }

    public StringProperty defaultBaseURIProperty() {
        return defaultBaseURI;
    }

    public void setDefaultBaseURI(String defaultBaseURI) {
        this.defaultBaseURI.set(defaultBaseURI);
    }
}
