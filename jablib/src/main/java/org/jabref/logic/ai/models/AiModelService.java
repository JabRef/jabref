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
     * Synchronously fetches the list of available models from the API with a timeout.
     * This method will block until the fetch completes or times out.
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
                    FetchThread fetchThread = new FetchThread(provider, aiProvider, apiBaseUrl, apiKey);
                    fetchThread.start();
                    fetchThread.join(FETCH_TIMEOUT_SECONDS * 1000L);

                    if (fetchThread.isAlive()) {
                        fetchThread.interrupt();
                        LOGGER.debug("Timeout while fetching models for {}", aiProvider.getLabel());
                        return List.of();
                    }

                    if (fetchThread.getException() != null) {
                        LOGGER.debug("Failed to fetch models for {}: {}", aiProvider.getLabel(), fetchThread.getException().getMessage());
                        return List.of();
                    }

                    List<String> models = fetchThread.getResult();
                    if (models != null && !models.isEmpty()) {
                        return models;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.debug("Interrupted while fetching models for {}", aiProvider.getLabel());
                }
            }
        }

        return List.of();
    }

    /**
     * Helper thread class to perform the fetch operation with timeout support.
     */
    private static class FetchThread extends Thread {
        private final AiModelProvider provider;
        private final AiProvider aiProvider;
        private final String apiBaseUrl;
        private final String apiKey;
        private List<String> result;
        private Exception exception;

        FetchThread(AiModelProvider provider, AiProvider aiProvider, String apiBaseUrl, String apiKey) {
            this.provider = provider;
            this.aiProvider = aiProvider;
            this.apiBaseUrl = apiBaseUrl;
            this.apiKey = apiKey;
            setDaemon(true); // Don't prevent JVM shutdown
        }

        @Override
        public void run() {
            try {
                result = provider.fetchModels(aiProvider, apiBaseUrl, apiKey);
            } catch (Exception e) {
                this.exception = e;
            }
        }

        public List<String> getResult() {
            return result;
        }

        public Exception getException() {
            return exception;
        }
    }
}
