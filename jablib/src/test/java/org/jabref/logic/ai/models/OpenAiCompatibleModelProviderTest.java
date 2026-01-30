package org.jabref.logic.ai.models;

import java.util.List;

import org.jabref.logic.net.URLDownload;
import org.jabref.model.ai.AiProvider;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiCompatibleModelProviderTest {

    private OpenAiCompatibleModelProvider provider;

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
        provider = new OpenAiCompatibleModelProvider();
    }

    @Test
    void supportsOpenAi() {
        assertTrue(provider.supports(AiProvider.OPEN_AI));
    }

    @Test
    void supportsMistralAi() {
        assertTrue(provider.supports(AiProvider.MISTRAL_AI));
    }

    @Test
    void supportsGpt4All() {
        assertTrue(provider.supports(AiProvider.GPT4ALL));
    }

    @Test
    void doesNotSupportOtherProviders() {
        assertFalse(provider.supports(AiProvider.HUGGING_FACE));
        assertFalse(provider.supports(AiProvider.GEMINI));
    }

    @Test
    void fetchModelsReturnsEmptyListWhenApiKeyIsNull() {
        List<String> models = provider.fetchModels(AiProvider.OPEN_AI, "https://api.openai.com", null);
        assertTrue(models.isEmpty());
    }

    @Test
    void fetchModelsReturnsEmptyListWhenApiKeyIsBlank() {
        List<String> models = provider.fetchModels(AiProvider.OPEN_AI, "https://api.openai.com", "   ");
        assertTrue(models.isEmpty());
    }

    @Test
    void fetchModelsReturnsEmptyListWhenApiKeyIsEmpty() {
        List<String> models = provider.fetchModels(AiProvider.OPEN_AI, "https://api.openai.com", "");
        assertTrue(models.isEmpty());
    }

    @Test
    void fetchModelsReturnsEmptyListForInvalidUrl() {
        // Using an invalid/unreachable URL should result in empty list
        List<String> models = provider.fetchModels(
                AiProvider.OPEN_AI,
                "https://this-is-an-invalid-url-that-does-not-exist-12345.com",
                "test-key"
        );
        assertTrue(models.isEmpty());
    }

    @Test
    void fetchModelsReturnsEmptyListForNonRoutableIp() {
        // Using a non-routable IP (TEST-NET-1) should timeout and return empty list
        // This tests the timeout functionality
        List<String> models = provider.fetchModels(
                AiProvider.OPEN_AI,
                "https://192.0.2.1",
                "test-key"
        );
        assertTrue(models.isEmpty());
    }

    @Test
    void fetchModelsHandlesInvalidUrlGracefully() {
        // Test with malformed URL structure
        List<String> models = provider.fetchModels(
                AiProvider.OPEN_AI,
                "not-a-valid-url",
                "test-key"
        );
        assertTrue(models.isEmpty());
    }

    @Test
    void fetchModelsHandlesMistralAiProvider() {
        // Test with blank API key for Mistral AI
        List<String> models = provider.fetchModels(
                AiProvider.MISTRAL_AI,
                "https://api.mistral.ai",
                ""
        );
        assertTrue(models.isEmpty());
    }

    @Test
    void fetchModelsHandlesGpt4AllProvider() {
        // Test with blank API key for GPT4ALL
        List<String> models = provider.fetchModels(
                AiProvider.GPT4ALL,
                "http://localhost:4891",
                ""
        );
        assertTrue(models.isEmpty());
    }

    @Test
    void supportsMethodReturnsFalseForUnsupportedProviders() {
        // Test all unsupported providers
        assertFalse(provider.supports(AiProvider.HUGGING_FACE),
                "Should not support HUGGING_FACE");
        assertFalse(provider.supports(AiProvider.GEMINI),
                "Should not support GEMINI");
    }

    @Test
    void supportsMethodReturnsTrueForAllSupportedProviders() {
        // Verify all supported providers
        assertTrue(provider.supports(AiProvider.OPEN_AI),
                "Should support OPEN_AI");
        assertTrue(provider.supports(AiProvider.MISTRAL_AI),
                "Should support MISTRAL_AI");
        assertTrue(provider.supports(AiProvider.GPT4ALL),
                "Should support GPT4ALL");
    }

    @Test
    void fetchModelsWithNullApiKeyForDifferentProviders() {
        // Test that all supported providers handle null API key correctly
        for (AiProvider provider : List.of(AiProvider.OPEN_AI, AiProvider.MISTRAL_AI, AiProvider.GPT4ALL)) {
            List<String> models = this.provider.fetchModels(provider, "https://example.com", null);
            assertTrue(models.isEmpty(),
                    "Provider " + provider + " should return empty list for null API key");
        }
    }

    @Test
    void fetchModelsWithBlankApiKeyForDifferentProviders() {
        // Test that all supported providers handle blank API key correctly
        for (AiProvider provider : List.of(AiProvider.OPEN_AI, AiProvider.MISTRAL_AI, AiProvider.GPT4ALL)) {
            List<String> models = this.provider.fetchModels(provider, "https://example.com", "   ");
            assertTrue(models.isEmpty(),
                    "Provider " + provider + " should return empty list for blank API key");
        }
    }
}
