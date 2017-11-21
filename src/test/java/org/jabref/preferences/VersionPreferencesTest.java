package org.jabref.preferences;

import org.jabref.logic.util.Version;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VersionPreferencesTest {

    @Test
    public void givenNothingWhenCreatingThenNothingThrown() {
        new VersionPreferences(Version.parse(""));
    }

    @Test
    public void givenIgnoredVersionStringWhenGetIgnoredVersionThenUnknown() {
        Version ignoredVersion = Version.parse("");
        VersionPreferences preferences = new VersionPreferences(ignoredVersion);
        assertEquals("*unknown*", preferences.getIgnoredVersion().toString());
    }
}
