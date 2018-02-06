package org.jabref.preferences;

import java.io.File;
import java.util.Objects;

public class LastFocusedTabPreferences {

    private final JabRefPreferences preferences;

    public LastFocusedTabPreferences(JabRefPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    public void setLastFocusedTab(File file) {
        if (file == null) {
            return;
        }

        String filePath = file.getAbsolutePath();
        preferences.put(JabRefPreferences.LAST_FOCUSED, filePath);
    }

    public boolean hadLastFocus(File file) {
        if (file == null) {
            return false;
        }

        String lastFocusedDatabase = preferences.get(JabRefPreferences.LAST_FOCUSED);
        return file.getAbsolutePath().equals(lastFocusedDatabase);
    }
}
