package org.jabref.gui.cleanup;

import java.util.Optional;

import org.jabref.logic.cleanup.CleanupPreferences;

public interface CleanupPanel {
    Optional<CleanupPreferences> getCleanupPreferences();
}
