package org.jabref.preferences;

import java.nio.file.Path;
import java.util.Objects;

public class LastFocusedTabPreferences {

    private final JabRefPreferences preferences;

    public LastFocusedTabPreferences(JabRefPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    public void setLastFocusedTab(Path path) {
        if (path == null) {
            return;
        }

        String filePath = path.toAbsolutePath().toString();
        preferences.put(JabRefPreferences.LAST_FOCUSED, filePath);
    }

    public boolean hadLastFocus(Path path) {
        if (path == null) {
            return false;
        }

        String lastFocusedDatabase = preferences.get(JabRefPreferences.LAST_FOCUSED);
        return path.toAbsolutePath().toString().equals(lastFocusedDatabase);
    }
}
