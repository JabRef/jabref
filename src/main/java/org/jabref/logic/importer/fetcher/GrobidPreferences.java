package org.jabref.logic.importer.fetcher;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GrobidPreferences {
    private final BooleanProperty grobidEnabled;
    private final BooleanProperty grobidOptOut;
    private final BooleanProperty grobidDemanded;
    private final StringProperty grobidURL;

    public GrobidPreferences(boolean grobidEnabled,
                             boolean grobidOptOut,
                             boolean grobidDemanded,
                             String grobidURL) {
        this.grobidEnabled = new SimpleBooleanProperty(grobidEnabled);
        this.grobidOptOut = new SimpleBooleanProperty(grobidOptOut);
        this.grobidDemanded = new SimpleBooleanProperty(grobidDemanded);
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

    public boolean isGrobidOptOut() {
        return grobidOptOut.get();
    }

    public BooleanProperty grobidOptOutProperty() {
        return grobidOptOut;
    }

    public void setGrobidOptOut(boolean grobidOptOut) {
        this.grobidOptOut.set(grobidOptOut);
    }

    public boolean isGrobidDemanded() {
        return grobidDemanded.get();
    }

    public BooleanProperty grobidDemandedProperty() {
        return grobidDemanded;
    }

    public void setGrobidDemanded(boolean grobidDemanded) {
        this.grobidDemanded.set(grobidDemanded);
    }


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
