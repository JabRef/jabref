package org.jabref.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.util.Version;

public class InternalPreferences {

    private final ObjectProperty<Version> ignoredVersion;
    private final StringProperty user;

    public InternalPreferences(Version ignoredVersion, String user) {
        this.ignoredVersion = new SimpleObjectProperty<>(ignoredVersion);
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

    public String getUser() {
        return user.get();
    }
}
