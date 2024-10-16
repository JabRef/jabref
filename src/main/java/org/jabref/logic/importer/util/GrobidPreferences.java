package org.jabref.logic.importer.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GrobidPreferences {
    private final BooleanProperty grobidEnabled;
    private final BooleanProperty grobidPreference;
    private final StringProperty grobidURL;

    public GrobidPreferences(boolean grobidEnabled,
                             boolean grobidPreference,
                             String grobidURL) {
        this.grobidEnabled = new SimpleBooleanProperty(grobidEnabled);
        this.grobidPreference = new SimpleBooleanProperty(grobidPreference);
        this.grobidURL = new SimpleStringProperty(grobidURL);
    }

    public boolean isGrobidEnabled() {
        return grobidEnabled.get();
    }

    public BooleanProperty grobidEnabledProperty() {
        return grobidEnabled;
    }

    public void setGrobidEnabled(boolean grobidEnabled) {
        this.grobidEnabled.set(grobidEnabled);
    }

    // region: preference; models "Save Preference" option
    public boolean isGrobidPreference() {
        return grobidPreference.get();
    }

    public BooleanProperty grobidPreferenceProperty() {
        return grobidPreference;
    }

    public void setGrobidPreference(boolean grobidPreference) {
        this.grobidPreference.set(grobidPreference);
    }
    // endregion: optout

    public String getGrobidURL() {
        return grobidURL.get();
    }

    public StringProperty grobidURLProperty() {
        return grobidURL;
    }

    public void setGrobidURL(String grobidURL) {
        this.grobidURL.set(grobidURL);
    }
}
