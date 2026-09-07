package org.jabref.logic.preferences;

import org.jabref.logic.ai.preferences.AiDefaultExpertSettings;
import org.jabref.model.ai.pipeline.ResponseEngineKind;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreferenceMigrationTest {
    private static final String AI_ANSWER_ENGINE_KIND = "aiAnswerEngineKind";
    private static final String AI_RESPONSE_ENGINE_KIND = "aiResponseEngineKind";
    private static final String AI_CUSTOMIZE_SETTINGS = "aiCustomizeSettings";
    private static final String AI_DOCUMENT_SPLITTER_CHUNK_SIZE = "aiDocumentSplitterChunkSize";
    private static final String UNUSED_DEFAULT_VALUE = "";
    private static final int UNUSED_DEFAULT_INT_VALUE = 0;
    private static final boolean UNUSED_DEFAULT_BOOLEAN_VALUE = false;

    private boolean hasLegacyResponseEngineKindValue;
    private boolean hasResponseEngineKindValue;
    private String legacyResponseEngineKindValue;
    private String responseEngineKindValue;

    private boolean hasCustomizeSettingsValue;
    private boolean customizeSettingsValue;

    private boolean hasDocumentSplitterChunkSizeValue;
    private int documentSplitterChunkSizeValue;

    @BeforeEach
    void setUp() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();

        hasLegacyResponseEngineKindValue = preferences.hasKey(AI_ANSWER_ENGINE_KIND);
        hasResponseEngineKindValue = preferences.hasKey(AI_RESPONSE_ENGINE_KIND);
        legacyResponseEngineKindValue = preferences.get(AI_ANSWER_ENGINE_KIND, UNUSED_DEFAULT_VALUE);
        responseEngineKindValue = preferences.get(AI_RESPONSE_ENGINE_KIND, UNUSED_DEFAULT_VALUE);

        hasCustomizeSettingsValue = preferences.hasKey(AI_CUSTOMIZE_SETTINGS);
        customizeSettingsValue = preferences.getBoolean(AI_CUSTOMIZE_SETTINGS, UNUSED_DEFAULT_BOOLEAN_VALUE);

        hasDocumentSplitterChunkSizeValue = preferences.hasKey(AI_DOCUMENT_SPLITTER_CHUNK_SIZE);
        documentSplitterChunkSizeValue = preferences.getInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, UNUSED_DEFAULT_INT_VALUE);

        preferences.remove(AI_ANSWER_ENGINE_KIND);
        preferences.remove(AI_RESPONSE_ENGINE_KIND);
        preferences.remove(AI_CUSTOMIZE_SETTINGS);
        preferences.remove(AI_DOCUMENT_SPLITTER_CHUNK_SIZE);
    }

    @AfterEach
    void tearDown() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();

        restorePreference(preferences, AI_ANSWER_ENGINE_KIND, hasLegacyResponseEngineKindValue, legacyResponseEngineKindValue);
        restorePreference(preferences, AI_RESPONSE_ENGINE_KIND, hasResponseEngineKindValue, responseEngineKindValue);
        restorePreference(preferences, AI_CUSTOMIZE_SETTINGS, hasCustomizeSettingsValue, customizeSettingsValue);
        restorePreference(preferences, AI_DOCUMENT_SPLITTER_CHUNK_SIZE, hasDocumentSplitterChunkSizeValue, documentSplitterChunkSizeValue);
    }

    @Test
    void getAiPreferencesMigratesLegacyResponseEngineKind() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.put(AI_ANSWER_ENGINE_KIND, ResponseEngineKind.FULL_DOCUMENT.name());

        ResponseEngineKind responseEngineKind = preferences.getAiPreferences().getResponseEngineKind();

        assertEquals(ResponseEngineKind.FULL_DOCUMENT, responseEngineKind);
        assertEquals(ResponseEngineKind.FULL_DOCUMENT.name(), preferences.get(AI_RESPONSE_ENGINE_KIND, UNUSED_DEFAULT_VALUE));
    }

    @Test
    void getAiPreferencesKeepsNewResponseEngineKindWhenLegacyValueExists() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.put(AI_ANSWER_ENGINE_KIND, ResponseEngineKind.FULL_DOCUMENT.name());
        preferences.put(AI_RESPONSE_ENGINE_KIND, ResponseEngineKind.EMBEDDINGS_SEARCH.name());

        ResponseEngineKind responseEngineKind = preferences.getAiPreferences().getResponseEngineKind();

        assertEquals(ResponseEngineKind.EMBEDDINGS_SEARCH, responseEngineKind);
        assertEquals(ResponseEngineKind.EMBEDDINGS_SEARCH.name(), preferences.get(AI_RESPONSE_ENGINE_KIND, UNUSED_DEFAULT_VALUE));
    }

    @Test
    void getAiPreferencesClampsDocumentSplitterChunkSizeWhenExceedsMaximum() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.putBoolean(AI_CUSTOMIZE_SETTINGS, true);
        preferences.putInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, 600);

        assertEquals(AiDefaultExpertSettings.DOCUMENT_SPLITTER_MAX_CHUNK_SIZE, preferences.getAiPreferences().getDocumentSplitterChunkSize());
        assertEquals(AiDefaultExpertSettings.DOCUMENT_SPLITTER_MAX_CHUNK_SIZE, preferences.getAiPreferences().documentSplitterChunkSizeProperty().get());
        assertEquals(AiDefaultExpertSettings.DOCUMENT_SPLITTER_MAX_CHUNK_SIZE, preferences.getInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, UNUSED_DEFAULT_INT_VALUE));
    }

    @Test
    void getAiPreferencesKeepsDocumentSplitterChunkSizeWhenWithinLimit() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.putBoolean(AI_CUSTOMIZE_SETTINGS, true);
        preferences.putInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, 400);

        assertEquals(400, preferences.getAiPreferences().getDocumentSplitterChunkSize());
        assertEquals(400, preferences.getAiPreferences().documentSplitterChunkSizeProperty().get());
        assertEquals(400, preferences.getInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, UNUSED_DEFAULT_INT_VALUE));
    }

    @Test
    void getAiPreferencesClampsDocumentSplitterChunkSizePropertyEvenWhenExpertSettingsNotCustomized() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.putInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, 600);

        assertEquals(AiDefaultExpertSettings.DOCUMENT_SPLITTER_CHUNK_SIZE, preferences.getAiPreferences().getDocumentSplitterChunkSize());
        assertEquals(AiDefaultExpertSettings.DOCUMENT_SPLITTER_MAX_CHUNK_SIZE, preferences.getAiPreferences().documentSplitterChunkSizeProperty().get());
        assertEquals(AiDefaultExpertSettings.DOCUMENT_SPLITTER_MAX_CHUNK_SIZE, preferences.getInt(AI_DOCUMENT_SPLITTER_CHUNK_SIZE, UNUSED_DEFAULT_INT_VALUE));
    }

    private void restorePreference(JabRefCliPreferences preferences, String key, boolean hasValue, String value) {
        if (!hasValue) {
            preferences.remove(key);
            return;
        }

        preferences.put(key, value);
    }

    private void restorePreference(JabRefCliPreferences preferences, String key, boolean hasValue, int value) {
        if (!hasValue) {
            preferences.remove(key);
            return;
        }

        preferences.putInt(key, value);
    }

    private void restorePreference(JabRefCliPreferences preferences, String key, boolean hasValue, boolean value) {
        if (!hasValue) {
            preferences.remove(key);
            return;
        }

        preferences.putBoolean(key, value);
    }
}
