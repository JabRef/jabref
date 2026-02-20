package org.jabref.logic.preferences;

import java.nio.file.Path;
import java.util.List;
import java.util.prefs.BackingStoreException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
class JabRefCliPreferencesTest {
    @Test
    void storeFileHistoryWhenStickModeIsOn() throws BackingStoreException {
        var preferences = new JabRefCliPreferences();
        preferences.clear();
        preferences.getInternalPreferences().setMemoryStickMode(true);
        Path relativePath = Path.of("bib", "nour.bib");
        Path absoultePath = relativePath.toAbsolutePath();
        preferences.getLastFilesOpenedPreferences().getFileHistory().add(absoultePath);
        List<String> storedList = preferences.getStringList("recentDatabases");
        assertEquals(relativePath, Path.of(storedList.getFirst()));
    }

    @Test
    void storeFileHistoryWhenStickModeIsOff() throws BackingStoreException {
        var preferences = new JabRefCliPreferences();
        preferences.clear();
        preferences.getInternalPreferences().setMemoryStickMode(false);
        Path relativePath = Path.of("bib", "nour.bib");
        Path absoultePath = relativePath.toAbsolutePath();
        preferences.getLastFilesOpenedPreferences().getFileHistory().add(absoultePath);
        List<String> storedList = preferences.getStringList("recentDatabases");
        assertEquals(absoultePath, Path.of(storedList.getFirst()));
    }
}
