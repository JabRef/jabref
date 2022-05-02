package org.jabref.logic.importer;

import java.util.Set;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.jabref.logic.preferences.FetcherApiKey;

public class ImporterPreferences {

    private final BooleanProperty generateNewKeyOnImport;
    private final BooleanProperty grobidEnabled;
    private final BooleanProperty grobidOptOut;
    private final StringProperty grobidURL;
    private final ObservableSet<FetcherApiKey> apiKeys;

    public ImporterPreferences(boolean generateNewKeyOnImport,
                               boolean grobidEnabled,
                               boolean grobidOptOut,
                               String grobidURL,
                               Set<FetcherApiKey> apiKeys) {
        this.generateNewKeyOnImport = new SimpleBooleanProperty(generateNewKeyOnImport);
        this.grobidEnabled = new SimpleBooleanProperty(grobidEnabled);
        this.grobidOptOut = new SimpleBooleanProperty(grobidOptOut);
        this.grobidURL = new SimpleStringProperty(grobidURL);
        this.apiKeys = FXCollections.observableSet(apiKeys);
    }

    public boolean isGenerateNewKeyOnImport() {
        return generateNewKeyOnImport.get();
    }

    public BooleanProperty generateNewKeyOnImportProperty() {
        return generateNewKeyOnImport;
    }

    public boolean isGrobidEnabled() {
        return grobidEnabled.get();
    }

    public BooleanProperty grobidEnabledProperty() {
        return grobidEnabled;
    }

    public boolean isGrobidOptOut() {
        return grobidOptOut.get();
    }

    public BooleanProperty grobidOptOutProperty() {
        return grobidOptOut;
    }

    public String getGrobidURL() {
        return grobidURL.get();
    }

    public StringProperty grobidURLProperty() {
        return grobidURL;
    }

    public void setGenerateNewKeyOnImport(boolean generateNewKeyOnImport) {
        this.generateNewKeyOnImport.set(generateNewKeyOnImport);
    }

    public void setGrobidEnabled(boolean grobidEnabled) {
        this.grobidEnabled.set(grobidEnabled);
    }

    public void setGrobidOptOut(boolean grobidOptOut) {
        this.grobidOptOut.set(grobidOptOut);
    }

    public void setGrobidURL(String grobidURL) {
        this.grobidURL.set(grobidURL);
    }

    public ObservableSet<FetcherApiKey> getApiKeys() {
        return apiKeys;
    }
}
