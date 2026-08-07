package org.jabref.logic.net.ssl;

import java.nio.file.Path;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.util.Directories;

public class SSLPreferences {
    private final ObjectProperty<Path> truststorePath;

    private SSLPreferences() {
        this(
                Directories.getSslDirectory().resolve("truststore.jks")  // Truststore path
        );
    }

    public SSLPreferences(Path truststorePath) {
        this.truststorePath = new SimpleObjectProperty<>(truststorePath);
    }

    public static SSLPreferences getDefault() {
        return new SSLPreferences();
    }

    public Path getTruststorePath() {
        return truststorePath.getValue();
    }

    public ObjectProperty<Path> truststorePathProperty() {
        return truststorePath;
    }
}
