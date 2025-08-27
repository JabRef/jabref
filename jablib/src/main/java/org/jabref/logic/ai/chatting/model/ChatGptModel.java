package org.jabref.logic.ai.chatting.model;

import java.time.Duration;
import java.util.List;

import org.jabref.logic.ai.AiPreferences;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChatGPT model implementation using langchain4j's native OpenAI support.
 * This provides a dedicated ChatGPT implementation with full langchain4j integration.
 */
public class ChatGptModel implements ChatModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatGptModel.class);

    private final OpenAiChatModel openAiChatModel;

    public ChatGptModel(AiPreferences aiPreferences) {
        LOGGER.debug("Creating ChatGPT model with model: {}", aiPreferences.getSelectedChatModel());
        
        OpenAiChatModel.Builder builder = OpenAiChatModel.builder()
                .apiKey(aiPreferences.getApiKeyForAiProvider(aiPreferences.getAiProvider()))
                .modelName(aiPreferences.getSelectedChatModel())
                .temperature(aiPreferences.getTemperature())
                .timeout(Duration.ofMinutes(2))
                .maxRetries(3)
                .logRequests(true)
                .logResponses(true);
        
        // Support custom API base URLs for OpenAI-compatible endpoints
        String baseUrl = aiPreferences.getSelectedApiBaseUrl();
        if (!baseUrl.equals("https://api.openai.com/v1")) {
            builder.baseUrl(baseUrl);
        }
        
        this.openAiChatModel = builder.build();
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages) {
        LOGGER.debug("Generating response from ChatGPT model with {} messages", messages.size());
        return openAiChatModel.chat(messages);
    }
}