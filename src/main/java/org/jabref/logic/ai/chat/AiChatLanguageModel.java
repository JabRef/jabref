package org.jabref.logic.ai.chat;

import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.preferences.AiPreferences;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.jspecify.annotations.Nullable;

/**
 * Wrapper around langchain4j chat language model.
 * <p>
 * This class listens to preferences changes.
 */
public class AiChatLanguageModel {
    private final AiPreferences aiPreferences;

    private final ObjectProperty<@Nullable ChatLanguageModel> chatLanguageModelObjectProperty = new SimpleObjectProperty<>();

    public AiChatLanguageModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        if (aiPreferences.getEnableChatWithFiles()) {
            rebuild();
        }

        listenToPreferences();
    }

    public ObjectProperty<@Nullable ChatLanguageModel> chatLanguageModelObjectProperty() {
        return chatLanguageModelObjectProperty;
    }

    public @Nullable ChatLanguageModel getChatLanguageModel() {
        return chatLanguageModelObjectProperty.get();
    }

    private void rebuild() {
        ChatLanguageModel chatLanguageModel = null;

        switch (aiPreferences.getAiProvider()) {
            case OPEN_AI -> chatLanguageModel = OpenAiChatModel
                    .builder()
                    .apiKey(aiPreferences.getApiToken())
                    .modelName(aiPreferences.getChatModel())
                    .temperature(aiPreferences.getTemperature())
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            case MISTRAL_AI -> chatLanguageModel = MistralAiChatModel
                    .builder()
                    .apiKey(aiPreferences.getApiToken())
                    .modelName(aiPreferences.getChatModel())
                    .temperature(aiPreferences.getTemperature())
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            case HUGGING_FACE -> chatLanguageModel = HuggingFaceChatModel
                    .builder()
                    .accessToken(aiPreferences.getApiToken())
                    .modelId(aiPreferences.getChatModel())
                    .temperature(aiPreferences.getTemperature())
                    .build();
        }

        chatLanguageModelObjectProperty.set(chatLanguageModel);
    }

    private void listenToPreferences() {
        aiPreferences.enableChatWithFilesProperty().addListener(obs -> {
            if (aiPreferences.getEnableChatWithFiles()) {
                if (!aiPreferences.getApiToken().isEmpty()) {
                    rebuild();
                }
            } else {
                chatLanguageModelObjectProperty.set(null);
            }
        });

        aiPreferences.apiTokenProperty().addListener(obs -> {
            if (aiPreferences.getEnableChatWithFiles()) {
                rebuild();
            } else {
                chatLanguageModelObjectProperty.set(null);
            }
        });

        aiPreferences.chatModelProperty().addListener(obs -> rebuild());

        aiPreferences.temperatureProperty().addListener(obs -> rebuild());
    }
}
