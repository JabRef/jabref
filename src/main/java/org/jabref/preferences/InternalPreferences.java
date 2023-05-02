package org.jabref.preferences;

import java.nio.file.Path;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.util.Version;

public class InternalPreferences {

    private final ObjectProperty<Version> ignoredVersion;
    private final ObjectProperty<Path> lastPreferencesExportPath;
    private final StringProperty user;

    public InternalPreferences(Version ignoredVersion, Path exportPath, String user) {
        this.ignoredVersion = new SimpleObjectProperty<>(ignoredVersion);
        this.lastPreferencesExportPath = new SimpleObjectProperty<>(exportPath);
        this.user = new SimpleStringProperty(user);
    }

    public Version getIgnoredVersion() {
        return ignoredVersion.getValue();
    }

    public ObjectProperty<Version> ignoredVersionProperty() {
        return ignoredVersion;
    }

    public void setIgnoredVersion(Version ignoredVersion) {
        this.ignoredVersion.set(ignoredVersion);
    }

    public Path getLastPreferencesExportPath() {
        return lastPreferencesExportPath.get();
    }

    public ObjectProperty<Path> lastPreferencesExportPathProperty() {
        return lastPreferencesExportPath;
    }

    public void setLastPreferencesExportPath(Path lastPreferencesExportPath) {
        this.lastPreferencesExportPath.set(lastPreferencesExportPath);
    }

    public String getUser() {
        return user.get();
    }
}
