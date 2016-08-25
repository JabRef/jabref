package net.sf.jabref.preferences;

import net.sf.jabref.logic.util.Version;


public class VersionPreferences {

    private final JabRefPreferences preferences;


    public VersionPreferences(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    public void setAsIgnoredVersion(Version version) {
        preferences.put(JabRefPreferences.VERSION_IGNORED_UPDATE, version.toString());
    }

    public Version getIgnoredVersion() {
        return new Version(preferences.get(JabRefPreferences.VERSION_IGNORED_UPDATE));
    }

}
