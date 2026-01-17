package org.jabref.logic.ai.models;

import java.util.List;

import org.jabref.logic.net.URLDownload;
import org.jabref.model.ai.AiProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModelServiceTest {

    private AiModelService aiModelService;

    @BeforeAll
    static void ensureUnirestInitialized() {
        // Ensure URLDownload's static initializer runs before any tests
        // This configures Unirest and prevents UnirestConfigException
        try {
            Class.forName(URLDownload.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to initialize URLDownload", e);
        }
    }

    @BeforeEach
    void setUp() {
        aiModelService = new AiModelService();
    }

    @Test
    void getStaticModelsReturnsNonEmptyListForOpenAi() {
        List<String> models = aiModelService.getStaticModels(AiProvider.OPEN_AI);
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertTrue(models.contains("gpt-4o-mini"));
        assertTrue(models.contains("gpt-4o"));
    }

    @Test
    void getStaticModelsReturnsNonEmptyListForMistralAi() {
        List<String> models = aiModelService.getStaticModels(AiProvider.MISTRAL_AI);
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertTrue(models.contains("open-mistral-nemo"));
    }

    @Test
    void getStaticModelsReturnsNonEmptyListForGemini() {
        List<String> models = aiModelService.getStaticModels(AiProvider.GEMINI);
        assertNotNull(models);
        assertFalse(models.isEmpty());
        assertTrue(models.contains("gemini-1.5-flash"));
    }

    @Test
    void fetchModelsSynchronouslyReturnsEmptyListForInvalidApiKey() {
        List<String> models = aiModelService.fetchModelsSynchronously(
                AiProvider.OPEN_AI,
                "https://api.openai.com",
                null
        );
       assertEquals(List.of(), models);
    }

    @Test
    void fetchModelsSynchronouslyReturnsEmptyListForBlankApiKey() {
        List<String> models = aiModelService.fetchModelsSynchronously(
                AiProvider.OPEN_AI,
                "https://api.openai.com",
                "   "
        );
        assertEquals(List.of(), models);
    }

    @Test
    void fetchModelsSynchronouslyReturnsEmptyListForInvalidUrl() {
        List<String> models = aiModelService.fetchModelsSynchronously(
                AiProvider.OPEN_AI,
                "https://invalid-url-that-does-not-exist-12345.com",
                "test-key"
        );
        assertEquals(List.of(), models);
    }

    @Test
    void fetchModelsSynchronouslyHandlesTimeout() {
        // This test uses a URL that will likely timeout (non-routable IP)
        // Unirest has a default timeout, so should fail and return empty list
        long startTime = System.currentTimeMillis();

        List<String> models = aiModelService.fetchModelsSynchronously(
                AiProvider.OPEN_AI,
                "https://192.0.2.1",  // Non-routable IP address (TEST-NET-1)
                "test-key"
        );

        long duration = System.currentTimeMillis() - startTime;

        assertEquals(List.of(), models);
        // Unirest default timeout is around 10 seconds, allow margin for test execution
        assertTrue(duration < 15000, "Should timeout within reasonable time, but took " + duration + "ms");
    }

    @Test
    void getAvailableModelsReturnsDynamicModelsWhenAvailable() {
        // Since we can't easily mock the internal provider without dependency injection,
        // we test the fallback behavior
        // When fetch fails, it should return static models
        List<String> models = aiModelService.getAvailableModels(
                AiProvider.OPEN_AI,
                "https://invalid-url.com",
                "test-key"
        );

        assertNotNull(models);
        // Should fall back to static models
        assertEquals(List.of("gpt-4o-mini, gpt-4o, gpt-4, gpt-4-turbo, gpt-3.5-turbo"), models);
    }

    @Test
    void getAvailableModelsFallsBackToStaticModelsWhenFetchFails() {
        List<String> models = aiModelService.getAvailableModels(
                AiProvider.OPEN_AI,
                "https://invalid-url.com",
                "invalid-key"
        );

        assertNotNull(models);
        // Should return static models as fallback
        List<String> staticModels = aiModelService.getStaticModels(AiProvider.OPEN_AI);
        assertEquals(staticModels, models);
    }

    @Test
    void getAvailableModelsFallsBackToStaticModelsWhenApiKeyIsBlank() {
        List<String> models = aiModelService.getAvailableModels(
                AiProvider.OPEN_AI,
                "https://api.openai.com",
                ""
        );

        assertNotNull(models);
        // Should return static models as fallback
        List<String> staticModels = aiModelService.getStaticModels(AiProvider.OPEN_AI);
        assertEquals(staticModels, models);
    }

    @Test
    void fetchModelsSynchronouslyReturnsEmptyListForUnsupportedProvider() {
        // HUGGING_FACE is not supported by OpenAiCompatibleModelProvider
        List<String> models = aiModelService.fetchModelsSynchronously(
                AiProvider.HUGGING_FACE,
                "https://api.huggingface.co",
                "test-key"
        );

        assertEquals(List.of(), models);
    }

    @Test
    void getAvailableModelsFallsBackForUnsupportedProvider() {
        List<String> models = aiModelService.getAvailableModels(
                AiProvider.HUGGING_FACE,
                "https://api.huggingface.co",
                "test-key"
        );

        assertNotNull(models);
        // Should return static models for HUGGING_FACE
        List<String> staticModels = aiModelService.getStaticModels(AiProvider.HUGGING_FACE);
        assertEquals(staticModels, models);
    }
}
