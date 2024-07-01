// PreferencesManager.java
package org.jabref.preferences;

import java.util.prefs.Preferences;

public class PreferencesManager {

    private static final int DEFAULT_MAX_HITS = 5;
    private Preferences prefs;

    public PreferencesManager() {
        prefs = Preferences.userNodeForPackage(PreferencesManager.class);
    }

    public int getMaxHits() {
        return prefs.getInt("maxHits", DEFAULT_MAX_HITS);
    }

    public void setMaxHits(int maxHits) {
        prefs.putInt("maxHits", maxHits);
    }
}
