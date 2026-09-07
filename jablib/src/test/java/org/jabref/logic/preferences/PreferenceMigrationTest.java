package org.jabref.logic.preferences;

import org.jabref.model.ai.pipeline.ResponseEngineKind;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreferenceMigrationTest {
    private static final String AI_ANSWER_ENGINE_KIND = "aiAnswerEngineKind";
    private static final String AI_RESPONSE_ENGINE_KIND = "aiResponseEngineKind";
    private static final String AI_EMBEDDING_MODEL = "aiEmbeddingModel";
    private static final String UNUSED_DEFAULT_VALUE = "";

    private boolean hasLegacyResponseEngineKindValue;
    private boolean hasResponseEngineKindValue;
    private boolean hasEmbeddingModelValue;
    private String legacyResponseEngineKindValue;
    private String responseEngineKindValue;
    private String embeddingModelValue;

    @BeforeEach
    void setUp() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();

        hasLegacyResponseEngineKindValue = preferences.hasKey(AI_ANSWER_ENGINE_KIND);
        hasResponseEngineKindValue = preferences.hasKey(AI_RESPONSE_ENGINE_KIND);
        hasEmbeddingModelValue = preferences.hasKey(AI_EMBEDDING_MODEL);
        legacyResponseEngineKindValue = preferences.get(AI_ANSWER_ENGINE_KIND, UNUSED_DEFAULT_VALUE);
        responseEngineKindValue = preferences.get(AI_RESPONSE_ENGINE_KIND, UNUSED_DEFAULT_VALUE);
        embeddingModelValue = preferences.get(AI_EMBEDDING_MODEL, UNUSED_DEFAULT_VALUE);

        preferences.remove(AI_ANSWER_ENGINE_KIND);
        preferences.remove(AI_RESPONSE_ENGINE_KIND);
        preferences.remove(AI_EMBEDDING_MODEL);
    }

    @AfterEach
    void tearDown() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();

        restorePreference(preferences, AI_ANSWER_ENGINE_KIND, hasLegacyResponseEngineKindValue, legacyResponseEngineKindValue);
        restorePreference(preferences, AI_RESPONSE_ENGINE_KIND, hasResponseEngineKindValue, responseEngineKindValue);
        restorePreference(preferences, AI_EMBEDDING_MODEL, hasEmbeddingModelValue, embeddingModelValue);
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
    void getAiPreferencesMigratesLegacyEmbeddingModelL12() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.put(AI_EMBEDDING_MODEL, "SENTENCE_TRANSFORMERS_ALL_MINILM_L12_V2");

        String embeddingModel = preferences.getAiPreferences().getEmbeddingModel();

        assertEquals("sentence-transformers/all-MiniLM-L12-v2", embeddingModel);
        assertEquals("sentence-transformers/all-MiniLM-L12-v2", preferences.get(AI_EMBEDDING_MODEL, UNUSED_DEFAULT_VALUE));
    }

    @Test
    void getAiPreferencesMigratesLegacyEmbeddingModelL6() {
        JabRefCliPreferences preferences = new JabRefCliPreferences();
        preferences.put(AI_EMBEDDING_MODEL, "SENTENCE_TRANSFORMERS_ALL_MINILM_L6_V2");

        String embeddingModel = preferences.getAiPreferences().getEmbeddingModel();

        assertEquals("sentence-transformers/all-MiniLM-L6-v2", embeddingModel);
        assertEquals("sentence-transformers/all-MiniLM-L6-v2", preferences.get(AI_EMBEDDING_MODEL, UNUSED_DEFAULT_VALUE));
    }

    private void restorePreference(JabRefCliPreferences preferences, String key, boolean hasValue, String value) {
        if (!hasValue) {
            preferences.remove(key);
            return;
        }

        preferences.put(key, value);
    }
}
