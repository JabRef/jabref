package org.jabref.logic.ai.models;

import java.util.ArrayList;
import java.util.List;

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

    private final List<AiModelProvider> modelProviders = List.of(new OpenAiCompatibleModelProvider());

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
        List<String> dynamicModels = fetchModelsSynchronously(aiProvider, apiBaseUrl, apiKey);

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
     * Synchronously fetches the list of available models from the API.
     * This method will block until the fetch completes or the HTTP client times out.
     *
     * @param aiProvider The AI provider
     * @param apiBaseUrl The base URL for the API
     * @param apiKey The API key for authentication
     * @return A list of model names, or an empty list if the fetch fails
     */
    public List<String> fetchModelsSynchronously(AiProvider aiProvider, String apiBaseUrl, String apiKey) {
        for (AiModelProvider provider : modelProviders) {
            if (provider.supports(aiProvider)) {
                try {
                    List<String> models = provider.fetchModels(aiProvider, apiBaseUrl, apiKey);
                    if (models != null && !models.isEmpty()) {
                        return models;
                    }
                } catch (Exception e) {
                    LOGGER.debug("Failed to fetch models for {}", aiProvider.getLabel(), e);
                }
            }
        }

        return List.of();
    }
}
