package org.jabref.logic.importer.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GrobidPreferences {

    private final BooleanProperty grobidEnabled;
    private final BooleanProperty grobidUseAsked;
    private final StringProperty grobidURL;

    private GrobidPreferences() {
        this(
                false,                          // Grobid enabled
                false,                          // Grobid use asked
                "http://grobid.jabref.org:8070" // Grobid URL
        );
    }

    public GrobidPreferences(boolean grobidEnabled,
                             boolean grobidUseAsked,
                             String grobidURL) {
        this.grobidEnabled = new SimpleBooleanProperty(grobidEnabled);
        this.grobidUseAsked = new SimpleBooleanProperty(grobidUseAsked);
        this.grobidURL = new SimpleStringProperty(grobidURL);
    }

    public static GrobidPreferences getDefault() {
        return new GrobidPreferences();
    }

    public void setAll(GrobidPreferences preferences) {
        this.grobidEnabled.set(preferences.isGrobidEnabled());
        this.grobidUseAsked.set(preferences.isGrobidUseAsked());
        this.grobidURL.set(preferences.getGrobidURL());
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

    // region: GrobidUseAsked;
    public boolean isGrobidUseAsked() {
        return grobidUseAsked.get();
    }

    public BooleanProperty grobidUseAskedProperty() {
        return grobidUseAsked;
    }

    public void setGrobidUseAsked(boolean grobidUseAsked) {
        this.grobidUseAsked.set(grobidUseAsked);
    }
    // endregion: GrobidUseAsked

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
