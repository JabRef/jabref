package org.jabref.logic;

import java.nio.file.Path;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.util.Version;
import org.jabref.model.metadata.UserHostInfo;

public class InternalPreferences {

    private final ObjectProperty<Version> ignoredVersion;
    private final BooleanProperty versionCheckEnabled;
    private final ObjectProperty<Path> lastPreferencesExportPath;
    private final ObjectProperty<UserHostInfo> userAndHost;
    private final BooleanProperty memoryStickMode;

    public InternalPreferences(Version ignoredVersion,
                               boolean versionCheck,
                               Path exportPath,
                               UserHostInfo userAndHost,
                               boolean memoryStickMode) {
        this.ignoredVersion = new SimpleObjectProperty<>(ignoredVersion);
        this.versionCheckEnabled = new SimpleBooleanProperty(versionCheck);
        this.lastPreferencesExportPath = new SimpleObjectProperty<>(exportPath);
        this.userAndHost = new SimpleObjectProperty<>(userAndHost);
        this.memoryStickMode = new SimpleBooleanProperty(memoryStickMode);
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

    public String getUserAndHost() {
        return userAndHost.get().getUserHostString();
    }

    public ObjectProperty<UserHostInfo> getUserAndHostProperty() {
        return userAndHost;
    }

    /// Returns the user and host information as a UserHostInfo object.
    ///
    /// @return the user and host information
    public UserHostInfo getUserHostInfo() {
        return userAndHost.get();
    }

    /// Sets the user and host information from a UserHostInfo object.
    ///
    /// @param userHostInfo the user and host information
    public void setUserHostInfo(UserHostInfo userHostInfo) {
        userAndHost.set(userHostInfo);
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
