package org.jabref.logic.net.ssl;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SSLPreferences {
    private final StringProperty truststorePath;

    public SSLPreferences(String truststorePath) {
        this.truststorePath = new SimpleStringProperty(truststorePath);
    }

    public StringProperty truststorePathProperty() {
        return truststorePath;
    }

    public String getTruststorePath() {
        return truststorePath.getValue();
    }
}
