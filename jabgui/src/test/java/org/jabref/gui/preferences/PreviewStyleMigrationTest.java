package org.jabref.gui.preferences;

import java.util.List;

import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preview.CustomizedPreviewStyle;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PreviewStyleMigrationTest {

    private static final String[] CUSTOMIZED_SERIES_KEYS = {
            JabRefGuiPreferences.PREVIEW_STYLE_CUSTOMIZED_ID,
            JabRefGuiPreferences.PREVIEW_STYLE_CUSTOMIZED_NAME,
            JabRefGuiPreferences.PREVIEW_STYLE_CUSTOMIZED_TEXT
    };
    // Only a handful of entries are ever written in these tests; large enough to catch a leftover
    // series from a failed prior run without saving/restoring an unbounded range.
    private static final int MAX_SERIES_INDEX_TO_SAVE = 5;
    private static final String UNUSED_DEFAULT_VALUE = "";

    private boolean hasLegacyPreviewStyle;
    private String legacyPreviewStyleValue;
    private boolean hasMigratedFlag;
    private boolean migratedFlagValue;
    private boolean hasLegacyCycle;
    private String legacyCycleValue;
    private final boolean[] hasSeriesValue = new boolean[CUSTOMIZED_SERIES_KEYS.length * MAX_SERIES_INDEX_TO_SAVE];
    private final String[] seriesValue = new String[CUSTOMIZED_SERIES_KEYS.length * MAX_SERIES_INDEX_TO_SAVE];

    @BeforeEach
    void setUp() {
        Injector.setModelOrService(JournalAbbreviationRepository.class, mock(JournalAbbreviationRepository.class));
        Injector.setModelOrService(BibEntryTypesManager.class, mock(BibEntryTypesManager.class));

        JabRefGuiPreferences preferences = new JabRefGuiPreferences();

        hasLegacyPreviewStyle = preferences.hasKey(JabRefGuiPreferences.PREVIEW_STYLE);
        legacyPreviewStyleValue = preferences.get(JabRefGuiPreferences.PREVIEW_STYLE, UNUSED_DEFAULT_VALUE);
        hasMigratedFlag = preferences.hasKey(JabRefGuiPreferences.PREVIEW_STYLE_CUSTOMIZED_MIGRATED);
        migratedFlagValue = preferences.getBoolean(JabRefGuiPreferences.PREVIEW_STYLE_CUSTOMIZED_MIGRATED, false);

        for (int seriesIndex = 0; seriesIndex < CUSTOMIZED_SERIES_KEYS.length; seriesIndex++) {
            for (int i = 0; i < MAX_SERIES_INDEX_TO_SAVE; i++) {
                String key = CUSTOMIZED_SERIES_KEYS[seriesIndex] + i;
                int slot = (seriesIndex * MAX_SERIES_INDEX_TO_SAVE) + i;
                hasSeriesValue[slot] = preferences.hasKey(key);
                seriesValue[slot] = preferences.get(key, UNUSED_DEFAULT_VALUE);
            }
        }

        clearMigrationState(preferences);
        hasLegacyCycle = preferences.hasKey(JabRefGuiPreferences.PREVIEW_CYCLE);
        legacyCycleValue = preferences.get(JabRefGuiPreferences.PREVIEW_CYCLE, UNUSED_DEFAULT_VALUE);
        deleteIfPresent(preferences, JabRefGuiPreferences.PREVIEW_CYCLE);
    }

    @AfterEach
    void tearDown() {
        JabRefGuiPreferences preferences = new JabRefGuiPreferences();

        clearMigrationState(preferences);

        restorePreference(preferences, JabRefGuiPreferences.PREVIEW_STYLE, hasLegacyPreviewStyle, legacyPreviewStyleValue);
        restoreBoolean(preferences, JabRefGuiPreferences.PREVIEW_STYLE_CUSTOMIZED_MIGRATED, hasMigratedFlag, migratedFlagValue);
        for (int seriesIndex = 0; seriesIndex < CUSTOMIZED_SERIES_KEYS.length; seriesIndex++) {
            for (int i = 0; i < MAX_SERIES_INDEX_TO_SAVE; i++) {
                String key = CUSTOMIZED_SERIES_KEYS[seriesIndex] + i;
                int slot = (seriesIndex * MAX_SERIES_INDEX_TO_SAVE) + i;
                restorePreference(preferences, key, hasSeriesValue[slot], seriesValue[slot]);
            }
        }
        restorePreference(preferences, JabRefGuiPreferences.PREVIEW_CYCLE, hasLegacyCycle, legacyCycleValue);
    }

    private void clearMigrationState(JabRefGuiPreferences preferences) {
        deleteIfPresent(preferences, JabRefGuiPreferences.PREVIEW_STYLE);
        deleteIfPresent(preferences, JabRefGuiPreferences.PREVIEW_STYLE_CUSTOMIZED_MIGRATED);
        for (String seriesKey : CUSTOMIZED_SERIES_KEYS) {
            for (int i = 0; i < MAX_SERIES_INDEX_TO_SAVE; i++) {
                deleteIfPresent(preferences, seriesKey + i);
            }
        }
    }

    @Test
    void legacyPreviewStyleIsMigratedOnFirstLoad() {
        JabRefGuiPreferences preferences = new JabRefGuiPreferences();
        String legacyText = "<b>\\bibtextype</b>__NEWLINE__";
        preferences.put(JabRefGuiPreferences.PREVIEW_STYLE, legacyText);

        PreviewPreferences result = preferences.getPreviewPreferences();

        List<CustomizedPreviewStyle> customizedStyles = result.getCustomizedPreviewStyles();
        assertEquals(1, customizedStyles.size());
        assertEquals(TextBasedPreviewLayout.NAME, customizedStyles.getFirst().name());
        assertEquals(TextBasedPreviewLayout.NAME, customizedStyles.getFirst().id());
        assertEquals(legacyText.replace("__NEWLINE__", "\n"), customizedStyles.getFirst().text());
        assertTrue(preferences.getBoolean(JabRefGuiPreferences.PREVIEW_STYLE_CUSTOMIZED_MIGRATED, false));
    }

    @Test
    void migrationDoesNotResurrectLegacyStyleAfterUserDeletesAllCustomizedStyles() {
        JabRefGuiPreferences firstLoadPreferences = new JabRefGuiPreferences();
        String legacyText = "<b>\\bibtextype</b>__NEWLINE__";
        firstLoadPreferences.put(JabRefGuiPreferences.PREVIEW_STYLE, legacyText);

        PreviewPreferences firstLoad = firstLoadPreferences.getPreviewPreferences();
        assertEquals(1, firstLoad.getCustomizedPreviewStyles().size());

        // User deletes every customized style; the bound list listener persists the (now empty) series,
        // purging the numbered keys — including index 0 — but PREVIEW_STYLE_CUSTOMIZED_MIGRATED must survive that.
        firstLoad.getCustomizedPreviewStyles().clear();

        // Simulate an application restart against the same backing store.
        JabRefGuiPreferences secondLoadPreferences = new JabRefGuiPreferences();
        PreviewPreferences secondLoad = secondLoadPreferences.getPreviewPreferences();
        assertEquals(1, secondLoad.getCustomizedPreviewStyles().size());
        assertNotEquals(TextBasedPreviewLayout.NAME, secondLoad.getCustomizedPreviewStyles().getFirst().id());
        assertEquals(TextBasedPreviewLayout.NAME, secondLoad.getCustomizedPreviewStyles().getFirst().name());
        assertTrue(secondLoad.getCustomizedPreviewStyles().stream()
                             .noneMatch(style -> legacyText.replace("__NEWLINE__", "\n").equals(style.text())));
    }

    @Test
    void noLegacyKeyMeansNoMigrationAndDefaultStyleIsUsed() {
        JabRefGuiPreferences preferences = new JabRefGuiPreferences();
        PreviewPreferences result = preferences.getPreviewPreferences();

        assertEquals(1, result.getCustomizedPreviewStyles().size());
        assertNotEquals(TextBasedPreviewLayout.NAME, result.getCustomizedPreviewStyles().getFirst().id());
        assertEquals(TextBasedPreviewLayout.NAME, result.getCustomizedPreviewStyles().getFirst().name());
        assertEquals(TextBasedPreviewLayout.DEFAULT, result.getCustomizedPreviewStyles().getFirst().text());
        assertTrue(preferences.getBoolean(JabRefGuiPreferences.PREVIEW_STYLE_CUSTOMIZED_MIGRATED, false));
    }

    @Test
    void legacyCycleReferencingBuiltinPreviewNameStillResolvesAfterMigration() {
        JabRefGuiPreferences preferences = new JabRefGuiPreferences();
        String legacyText = "<b>\\bibtextype</b>__NEWLINE__";
        String oldCycle = TextBasedPreviewLayout.NAME + ";ieee.csl";
        preferences.put(JabRefGuiPreferences.PREVIEW_STYLE, legacyText);
        // Mirrors what PreferencesMigrations#upgradeBuiltinPreviewName would have already rewritten
        // a pre-upgrade cycle into, references GuiPreferencesMigrationsTest#previewStyleNameChanged
        preferences.put(JabRefGuiPreferences.PREVIEW_CYCLE, oldCycle);

        PreviewPreferences result = preferences.getPreviewPreferences();

        List<PreviewLayout> cycle = result.getLayoutCycle();
        TextBasedPreviewLayout migratedLayout = (TextBasedPreviewLayout) cycle.getFirst();
        assertEquals(2, cycle.size());
        assertInstanceOf(TextBasedPreviewLayout.class, migratedLayout);
        assertEquals(legacyText.replace("__NEWLINE__", "\n"), migratedLayout.getText());
        assertEquals(TextBasedPreviewLayout.NAME, migratedLayout.getName());
        assertEquals(TextBasedPreviewLayout.NAME, migratedLayout.getId());
    }

    private void deleteIfPresent(JabRefGuiPreferences preferences, String key) {
        if (preferences.hasKey(key)) {
            preferences.deleteKey(key);
        }
    }

    private void restorePreference(JabRefGuiPreferences preferences, String key, boolean hasValue, String value) {
        if (hasValue) {
            preferences.put(key, value);
        } else {
            deleteIfPresent(preferences, key);
        }
    }

    private void restoreBoolean(JabRefGuiPreferences preferences, String key, boolean hasValue, boolean value) {
        if (hasValue) {
            preferences.putBoolean(key, value);
        } else {
            deleteIfPresent(preferences, key);
        }
    }
}
