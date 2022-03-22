package org.jabref.logic.net.ssl;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SSLPreferences {
    private final BooleanProperty useCustomCertificates;
    private final StringProperty truststorePath;

    public SSLPreferences(Boolean useCustomCertificates, String truststorePath) {
        this.useCustomCertificates = new SimpleBooleanProperty(useCustomCertificates);
        this.truststorePath = new SimpleStringProperty(truststorePath);
    }

    public boolean shouldUseCustomCertificates() {
        return useCustomCertificates.getValue();
    }

    public BooleanProperty useCustomCertificatesProperty() {
        return useCustomCertificates;
    }

    public void setUseCustomCertificates(boolean useCustomCertificates) {
        this.useCustomCertificates.set(useCustomCertificates);
    }

    public StringProperty truststorePathProperty() {
        return truststorePath;
    }

    public String getTruststorePath() {
        return truststorePath.getValue();
    }

}
