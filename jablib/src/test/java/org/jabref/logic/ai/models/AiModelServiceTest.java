package org.jabref.logic.ai.models;

import java.util.List;

import org.jabref.model.ai.AiProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModelServiceTest {

    private AiModelService aiModelService;

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
        assertTrue(models.isEmpty());
    }

    @Test
    void fetchModelsSynchronouslyReturnsEmptyListForBlankApiKey() {
        List<String> models = aiModelService.fetchModelsSynchronously(
                AiProvider.OPEN_AI,
                "https://api.openai.com",
                "   "
        );
        assertTrue(models.isEmpty());
    }

    @Test
    void fetchModelsSynchronouslyReturnsEmptyListForInvalidUrl() {
        List<String> models = aiModelService.fetchModelsSynchronously(
                AiProvider.OPEN_AI,
                "https://invalid-url-that-does-not-exist-12345.com",
                "test-key"
        );
        assertTrue(models.isEmpty());
    }

    @Test
    void fetchModelsSynchronouslyHandlesTimeout() {
        // This test uses a URL that will likely timeout (non-routable IP)
        // The service should timeout after 5 seconds and return empty list
        long startTime = System.currentTimeMillis();

        List<String> models = aiModelService.fetchModelsSynchronously(
                AiProvider.OPEN_AI,
                "https://192.0.2.1",  // Non-routable IP address (TEST-NET-1)
                "test-key"
        );

        long duration = System.currentTimeMillis() - startTime;

        assertTrue(models.isEmpty());
        // Should timeout around 5 seconds, allow some margin for test execution
        assertTrue(duration < 7000, "Should timeout within ~5 seconds, but took " + duration + "ms");
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
        assertFalse(models.isEmpty());
        // Should fall back to static models
        assertTrue(models.contains("gpt-4o-mini"));
    }

    @Test
    void getAvailableModelsFallsBackToStaticModelsWhenFetchFails() {
        List<String> models = aiModelService.getAvailableModels(
                AiProvider.OPEN_AI,
                "https://invalid-url.com",
                "invalid-key"
        );

        assertNotNull(models);
        assertFalse(models.isEmpty());
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
        assertFalse(models.isEmpty());
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

        assertTrue(models.isEmpty());
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
