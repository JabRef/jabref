package org.jabref.preferences;

import org.jabref.logic.util.Version;

public class VersionPreferences {

    private final Version ignoredVersion;

    public VersionPreferences(Version ignoredVersion) {
        this.ignoredVersion = ignoredVersion;
    }

    public Version getIgnoredVersion() {
        return ignoredVersion;
    }
}
