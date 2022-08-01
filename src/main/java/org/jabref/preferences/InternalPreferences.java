package org.jabref.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.util.Version;

public class InternalPreferences {

    private final ObjectProperty<Version> ignoredVersion;
    private final ObjectProperty<Character> keywordSeparator;
    private final StringProperty user;

    public InternalPreferences(Version ignoredVersion, Character keywordSeparator, String user) {
        this.ignoredVersion = new SimpleObjectProperty<>(ignoredVersion);
        this.keywordSeparator = new SimpleObjectProperty<>(keywordSeparator);
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

    public Character getKeywordSeparator() {
        return keywordSeparator.get();
    }

    public ObjectProperty<Character> keywordSeparatorProperty() {
        return keywordSeparator;
    }

    public void setKeywordSeparator(Character keywordSeparator) {
        this.keywordSeparator.set(keywordSeparator);
    }

    public String getUser() {
        return user.get();
    }
}
