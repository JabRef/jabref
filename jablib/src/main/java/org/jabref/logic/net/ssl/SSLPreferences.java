package org.jabref.logic.net.ssl;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.util.Directories;

public class SSLPreferences {
    private final StringProperty truststorePath;

    private SSLPreferences() {
        this(
                Directories.getSslDirectory().resolve("truststore.jks").toString()  // Truststore path
        );
    }        // SSL

    public SSLPreferences(String truststorePath) {
        this.truststorePath = new SimpleStringProperty(truststorePath);
    }

    public static SSLPreferences getDefault() {
        return new SSLPreferences();
    }

    public void setAll(SSLPreferences preferences) {
        this.truststorePath.set(preferences.getTruststorePath());
    }

    public String getTruststorePath() {
        return truststorePath.getValue();
    }
}
