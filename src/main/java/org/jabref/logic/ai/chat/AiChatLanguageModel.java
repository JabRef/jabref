package org.jabref.logic.ai.chat;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.ai.chathistory.BibDatabaseChatHistory;
import org.jabref.preferences.AiPreferences;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.h2.mvstore.MVStore;

/**
 * Wrapper around langchain4j chat language model.
 * <p>
 * This class listens to preferences changes.
 */
public class AiChatLanguageModel {
    private final AiPreferences aiPreferences;

    private final ObjectProperty<Optional<ChatLanguageModel>> chatLanguageModelObjectProperty = new SimpleObjectProperty<>(Optional.empty());

    public AiChatLanguageModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        if (aiPreferences.getEnableChatWithFiles()) {
            rebuild();
        }

        setupListeningToPreferencesChanges();
    }

    public ObjectProperty<Optional<ChatLanguageModel>> chatLanguageModelObjectProperty() {
        return chatLanguageModelObjectProperty;
    }

    /**
     * Returns the chat language model.
     * The return may be empty in case user disallowed any usage of AI or if the API token is empty.
     * Unfortunately, we really mustn't send empty API tokens to langchain4j models, because there will be a
     * {@link RuntimeException}.
     */
    public Optional<ChatLanguageModel> getChatLanguageModel() {
        return chatLanguageModelObjectProperty.get();
    }

    /**
     * Update the underlying {@link ChatLanguageModel] by current {@link AiPreferences} parameters.
     * When the model is updated, the chat messages are not lost.
     * See {@link AiChatLogic}, where messages are stored in {@link ChatMemory},
     * and {@link BibDatabaseChatHistory}, where messages are stored in {@link MVStore}.
     */
    private void rebuild() {
        if (aiPreferences.getOpenAiToken().isEmpty()) {
            chatLanguageModelObjectProperty.set(null);
            return;
        }

        ChatLanguageModel chatLanguageModel =
                OpenAiChatModel
                        .builder()
                        .apiKey(aiPreferences.getOpenAiToken())
                        .modelName(aiPreferences.getChatModel().getLabel())
                        .temperature(aiPreferences.getTemperature())
                        .logRequests(true)
                        .logResponses(true)
                        .build();

        chatLanguageModelObjectProperty.set(Optional.of(chatLanguageModel));
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.enableChatWithFilesProperty().addListener(obs -> {
            if (aiPreferences.getEnableChatWithFiles()) {
                rebuild();
            } else {
                chatLanguageModelObjectProperty.set(null);
            }
        });

        aiPreferences.openAiTokenProperty().addListener(obs -> {
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
