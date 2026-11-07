package org.jabref.logic.ai.models;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.PredefinedChatModelUtil;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import dev.langchain4j.data.message.UserMessage;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Service for managing AI models from different providers.
/// Provides both static (hardcoded) and dynamic (API-fetched) model lists.
@NullMarked
public class AiModelService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiModelService.class);

    private final List<AiModelProvider> modelProviders = List.of(new OpenAiCompatibleModelProvider());

    /// Gets the list of available models for the given provider.
    /// First attempts to fetch models dynamically from the API.
    /// If that fails or times out, falls back to the hardcoded list.
    ///
    /// @param aiProvider The AI provider
    /// @param apiBaseUrl The base URL for the API
    /// @param apiKey     The API key for authentication (may be null)
    /// @return A list of available model names
    public List<String> getAvailableModels(AiProvider aiProvider, String apiBaseUrl, @Nullable String apiKey) {
        List<String> dynamicModels = fetchModelsSynchronously(aiProvider, apiBaseUrl, apiKey);

        if (!dynamicModels.isEmpty()) {
            LOGGER.debug("Using {} dynamic models for {}", dynamicModels.size(), aiProvider.name());
            return dynamicModels;
        }

        List<String> staticModels = getStaticModels(aiProvider);
        LOGGER.debug("Using {} hardcoded models for {}", staticModels.size(), aiProvider.name());
        return staticModels;
    }

    /// Gets the list of available models for the given provider, using only hardcoded values.
    ///
    /// @param aiProvider The AI provider
    /// @return A list of available model names
    public List<String> getStaticModels(AiProvider aiProvider) {
        return PredefinedChatModelUtil.getAvailableModels(aiProvider);
    }

    /// Sends a minimal chat request, which verifies provider, model name, API key, and API base URL in one call.
    /// A chat request is used instead of listing models, because it works for every provider.
    ///
    /// @return the reply of the model
    public String testConnection(AiProvider aiProvider, String modelName, String apiKey, double temperature, String apiBaseUrl, int contextWindowSize, TokenEstimatorKind tokenEstimatorKind) {
        try (ChatModel chatModel = ChatModelFactory.create(aiProvider, modelName, apiKey, temperature, apiBaseUrl, contextWindowSize, tokenEstimatorKind)) {
            return chatModel.chat(List.of(UserMessage.from("Reply with OK."))).aiMessage().text();
        }
    }

    /// Servers report a model they do not have with a "not found" message, e.g., Ollama: `model 'gpt-oss20b' not found`.
    public static boolean isModelNotFound(Exception exception) {
        return Optional.ofNullable(exception.getMessage())
                       .map(message -> message.contains("not found"))
                       .orElse(false);
    }

    /// Synchronously fetches the list of available models from the API.
    /// This method will block until the fetch completes or the HTTP client times out.
    ///
    /// @param aiProvider The AI provider
    /// @param apiBaseUrl The base URL for the API
    /// @param apiKey     The API key for authentication (may be null)
    /// @return A list of model names, or an empty list if the fetch fails
    public List<String> fetchModelsSynchronously(AiProvider aiProvider, String apiBaseUrl, @Nullable String apiKey) {
        for (AiModelProvider provider : modelProviders) {
            if (provider.supports(aiProvider)) {
                List<String> models = provider.fetchModels(aiProvider, apiBaseUrl, apiKey);
                if (!models.isEmpty()) {
                    return models;
                }
            }
        }

        return List.of();
    }
}
