package org.jabref.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePreferencesTest {

    @Test
    void shouldDownloadLinkedOnlineFilesDefaultsToTrue() {
        FilePreferences preferences = FilePreferences.getDefault();
        assertTrue(preferences.shouldDownloadLinkedOnlineFiles());
    }

    @Test
    void setDownloadLinkedOnlineFiles() {
        FilePreferences preferences = FilePreferences.getDefault();
        preferences.setDownloadLinkedOnlineFiles(false);
        assertFalse(preferences.shouldDownloadLinkedOnlineFiles());
    }
}
