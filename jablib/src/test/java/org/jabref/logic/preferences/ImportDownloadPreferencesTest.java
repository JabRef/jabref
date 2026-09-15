package org.jabref.logic.preferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.FilePreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("Preferences")
class ImportDownloadPreferencesTest {
    private static final String WEB_SEARCH_DOWNLOAD = "downloadLinkedFiles";
    private static final String IMPORT_DIALOG_DOWNLOAD = "importDialogDownloadLinkedFiles";

    private final Map<String, Optional<String>> savedPreferences = new HashMap<>();

    @BeforeEach
    void setUp() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        for (String key : new String[] {WEB_SEARCH_DOWNLOAD, IMPORT_DIALOG_DOWNLOAD}) {
            savedPreferences.put(key, preferences.hasKey(key) ? Optional.of(preferences.get(key, "")) : Optional.empty());
            preferences.remove(key);
        }
    }

    @AfterEach
    void tearDown() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        savedPreferences.forEach((key, value) -> value.ifPresentOrElse(
                stored -> preferences.put(key, stored),
                () -> preferences.remove(key)));
    }

    @Test
    void downloadsByDefaultForNewUsers() {
        assertTrue(new JabRefCliPreferences().getFilePreferences().shouldImportDialogDownloadLinkedFiles());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void migratesExistingChoiceOnlyOnce(boolean originalChoice) {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.putBoolean(WEB_SEARCH_DOWNLOAD, originalChoice);

        FilePreferences files = preferences.getFilePreferences();
        assertEquals(originalChoice, files.shouldImportDialogDownloadLinkedFiles());

        files.setDownloadLinkedFiles(!originalChoice);

        FilePreferences reloaded = new JabRefCliPreferences().getFilePreferences();
        assertEquals(originalChoice, reloaded.shouldImportDialogDownloadLinkedFiles());
        assertEquals(!originalChoice, reloaded.shouldDownloadLinkedFiles());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void persistsDialogChoiceWithoutChangingWebSearch(boolean choice) {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.putBoolean(WEB_SEARCH_DOWNLOAD, !choice);
        FilePreferences files = preferences.getFilePreferences();

        files.setImportDialogDownloadLinkedFiles(choice);

        FilePreferences reloaded = new JabRefCliPreferences().getFilePreferences();
        assertEquals(choice, reloaded.shouldImportDialogDownloadLinkedFiles());
        assertEquals(!choice, reloaded.shouldDownloadLinkedFiles());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void keepsPreviouslyStoredDialogChoice(boolean choice) {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.putBoolean(WEB_SEARCH_DOWNLOAD, !choice);
        preferences.putBoolean(IMPORT_DIALOG_DOWNLOAD, choice);

        assertEquals(choice, preferences.getFilePreferences().shouldImportDialogDownloadLinkedFiles());
    }
}
