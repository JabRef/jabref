package org.jabref.logic.ai.models;

import java.util.List;

import org.jabref.model.ai.AiProvider;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Interface for fetching available AI models from different providers.
 * Implementations should handle API calls to retrieve model lists dynamically.
 */
@NullMarked
public interface AiModelProvider {
    /**
     * Fetches the list of available models for the given AI provider.
     *
     * @param aiProvider The AI provider to fetch models from
     * @param apiBaseUrl The base URL for the API
     * @param apiKey The API key for authentication (may be null for providers that don't require it)
     * @return A list of available model names (never null, empty if fetch fails)
     */
    List<String> fetchModels(AiProvider aiProvider, String apiBaseUrl, @Nullable String apiKey);

    /**
     * Checks if this provider supports the given AI provider type.
     *
     * @param aiProvider The AI provider to check
     * @return true if this provider can fetch models for the given AI provider
     */
    boolean supports(AiProvider aiProvider);
}
