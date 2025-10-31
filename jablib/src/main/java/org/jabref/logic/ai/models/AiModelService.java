package org.jabref.logic.ai.models;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.ai.AiDefaultPreferences;
import org.jabref.model.ai.AiProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing AI models from different providers.
 * Provides both static (hardcoded) and dynamic (API-fetched) model lists.
 */
public class AiModelService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiModelService.class);
    private static final int FETCH_TIMEOUT_SECONDS = 5;

    private final List<AiModelProvider> modelProviders;

    public AiModelService() {
        this.modelProviders = new ArrayList<>();
        this.modelProviders.add(new OpenAiCompatibleModelProvider());
    }

    /**
     * Gets the list of available models for the given provider.
     * First attempts to fetch models dynamically from the API.
     * If that fails or times out, falls back to the hardcoded list.
     *
     * @param aiProvider The AI provider
     * @param apiBaseUrl The base URL for the API
     * @param apiKey The API key for authentication
     * @return A list of available model names
     */
    public List<String> getAvailableModels(AiProvider aiProvider, String apiBaseUrl, String apiKey) {
        List<String> dynamicModels = fetchModelsDynamically(aiProvider, apiBaseUrl, apiKey);

        if (!dynamicModels.isEmpty()) {
            LOGGER.info("Using {} dynamic models for {}", dynamicModels.size(), aiProvider.getLabel());
            return dynamicModels;
        }

        List<String> staticModels = AiDefaultPreferences.getAvailableModels(aiProvider);
        LOGGER.debug("Using {} hardcoded models for {}", staticModels.size(), aiProvider.getLabel());
        return staticModels;
    }

    /**
     * Gets the list of available models for the given provider, using only hardcoded values.
     *
     * @param aiProvider The AI provider
     * @return A list of available model names
     */
    public List<String> getStaticModels(AiProvider aiProvider) {
        return AiDefaultPreferences.getAvailableModels(aiProvider);
    }

    /**
     * Asynchronously fetches the list of available models from the API.
     *
     * @param aiProvider The AI provider
     * @param apiBaseUrl The base URL for the API
     * @param apiKey The API key for authentication
     * @return A CompletableFuture containing the list of model names
     */
    public CompletableFuture<List<String>> fetchModelsAsync(AiProvider aiProvider, String apiBaseUrl, String apiKey) {
        return CompletableFuture.supplyAsync(() -> fetchModelsDynamically(aiProvider, apiBaseUrl, apiKey));
    }

    private List<String> fetchModelsDynamically(AiProvider aiProvider, String apiBaseUrl, String apiKey) {
        for (AiModelProvider provider : modelProviders) {
            if (provider.supports(aiProvider)) {
                try {
                    CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(
                            () -> provider.fetchModels(aiProvider, apiBaseUrl, apiKey)
                    );

                    List<String> models = future.get(FETCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                    if (models != null && !models.isEmpty()) {
                        return models;
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to fetch models for {}: {}", aiProvider.getLabel(), e.getMessage());
                }
            }
        }

        return List.of();
    }
}
