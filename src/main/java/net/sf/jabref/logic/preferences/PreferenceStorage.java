package net.sf.jabref.logic.preferences;

import net.sf.jabref.logic.cleanup.CleanupPreset;

public interface PreferenceStorage {

    void storeCleanupPreset(CleanupPreset preset);

    CleanupPreset retrieveCleanupPreset();

}