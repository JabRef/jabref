package org.jabref.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePreferencesTest {

    @Test
    void shouldImportDialogDownloadLinkedFilesDefaultsToTrue() {
        FilePreferences preferences = FilePreferences.getDefault();
        assertTrue(preferences.shouldImportDialogDownloadLinkedFiles());
    }

    @Test
    void setImportDialogDownloadLinkedFiles() {
        FilePreferences preferences = FilePreferences.getDefault();
        preferences.setImportDialogDownloadLinkedFiles(false);
        assertFalse(preferences.shouldImportDialogDownloadLinkedFiles());
    }

    @Test
    void settingImportDialogPreferenceDoesNotAffectGeneralDownloadPreference() {
        FilePreferences preferences = FilePreferences.getDefault();
        boolean originalGeneralValue = preferences.shouldDownloadLinkedFiles();

        preferences.setImportDialogDownloadLinkedFiles(!preferences.shouldImportDialogDownloadLinkedFiles());

        assertEquals(originalGeneralValue, preferences.shouldDownloadLinkedFiles());
    }

    @Test
    void settingGeneralDownloadPreferenceDoesNotAffectImportDialogPreference() {
        FilePreferences preferences = FilePreferences.getDefault();
        boolean originalImportDialogValue = preferences.shouldImportDialogDownloadLinkedFiles();

        preferences.setDownloadLinkedFiles(!preferences.shouldDownloadLinkedFiles());

        assertEquals(originalImportDialogValue, preferences.shouldImportDialogDownloadLinkedFiles());
    }
}
