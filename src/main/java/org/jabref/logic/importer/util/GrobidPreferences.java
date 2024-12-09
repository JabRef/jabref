package org.jabref.logic.importer.util;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GrobidPreferences {
    private final BooleanProperty grobidEnabled;
    private final BooleanProperty grobidUseAsked;
    private final StringProperty grobidURL;

    public GrobidPreferences(boolean grobidEnabled,
                             boolean grobidUseAsked,
                             String grobidURL) {
        this.grobidEnabled = new SimpleBooleanProperty(grobidEnabled);
        this.grobidUseAsked = new SimpleBooleanProperty(grobidUseAsked);
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
