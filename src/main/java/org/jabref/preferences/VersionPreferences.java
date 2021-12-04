package org.jabref.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.util.Version;

public class VersionPreferences {

    private final ObjectProperty<Version> ignoredVersion;

    public VersionPreferences(Version ignoredVersion) {
        this.ignoredVersion = new SimpleObjectProperty<>(ignoredVersion);
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
}
