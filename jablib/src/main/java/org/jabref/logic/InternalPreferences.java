package org.jabref.logic;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.Version;
import org.jabref.model.metadata.UserHostInfo;

public class InternalPreferences {

    private final ObjectProperty<Version> ignoredVersion;
    private final BooleanProperty versionCheckEnabled;
    private final ObjectProperty<Path> lastPreferencesExportPath;
    private final ObjectProperty<UserHostInfo> userHostInfo;
    private final BooleanProperty memoryStickMode;

    private InternalPreferences() {
        this(
                Version.parse(""),                                     // No ignored version
                true,                                                  // Version check enabled
                Path.of(System.getProperty("user.home")),              // Preferences export path
                OS.getUserHostInfo(System.getProperty("user.name")),   // User and host
                false                                                  // Memory stick mode
        );
    }

    public InternalPreferences(Version ignoredVersion,
                               boolean versionCheck,
                               Path exportPath,
                               UserHostInfo userHostInfo,
                               boolean memoryStickMode) {
        this.ignoredVersion = new SimpleObjectProperty<>(ignoredVersion);
        this.versionCheckEnabled = new SimpleBooleanProperty(versionCheck);
        this.lastPreferencesExportPath = new SimpleObjectProperty<>(exportPath);
        this.userHostInfo = new SimpleObjectProperty<>(userHostInfo);
        this.memoryStickMode = new SimpleBooleanProperty(memoryStickMode);
    }

    public static InternalPreferences getDefault() {
        return new InternalPreferences();
    }

    public void setAll(InternalPreferences preferences) {
        this.ignoredVersion.set(preferences.getIgnoredVersion());
        this.versionCheckEnabled.set(preferences.isVersionCheckEnabled());
        this.lastPreferencesExportPath.set(preferences.getLastPreferencesExportPath());
        this.userHostInfo.set(preferences.getUserHostInfo());
        this.memoryStickMode.set(preferences.isMemoryStickMode());
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

    public boolean isVersionCheckEnabled() {
        return versionCheckEnabled.get();
    }

    public BooleanProperty versionCheckEnabledProperty() {
        return versionCheckEnabled;
    }

    public void setVersionCheckEnabled(boolean versionCheckEnabled) {
        this.versionCheckEnabled.set(versionCheckEnabled);
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

    public UserHostInfo getUserHostInfo() {
        return userHostInfo.get();
    }

    public ObjectProperty<UserHostInfo> getUserAndHostInfoProperty() {
        return userHostInfo;
    }

    public void setUserHostInfo(UserHostInfo userHostInfo) {
        this.userHostInfo.set(userHostInfo);
    }

    public boolean isMemoryStickMode() {
        return memoryStickMode.get();
    }

    public BooleanProperty memoryStickModeProperty() {
        return memoryStickMode;
    }

    public void setMemoryStickMode(boolean memoryStickMode) {
        this.memoryStickMode.set(memoryStickMode);
    }
}
