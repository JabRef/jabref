package org.jabref.logic.ai.chat;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.preferences.AiPreferences;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.jspecify.annotations.Nullable;

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
        ChatLanguageModel chatLanguageModel =
                OpenAiChatModel
                        .builder()
                        .apiKey(aiPreferences.getOpenAiToken())
                        .modelName(aiPreferences.getModel().getName())
                        .temperature(aiPreferences.getTemperature())
                        .logRequests(true)
                        .logResponses(true)
                        .build();

        chatLanguageModelObjectProperty.set(chatLanguageModel);
    }

    private void listenToPreferences() {
        aiPreferences.enableChatWithFilesProperty().addListener(obs -> {
            if (aiPreferences.getEnableChatWithFiles()) {
                if (!aiPreferences.getOpenAiToken().isEmpty()) {
                    rebuild();
                }
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

        aiPreferences.modelProperty().addListener(obs -> {
            rebuild();
        });

        aiPreferences.temperatureProperty().addListener(obs -> {
            rebuild();
        });
    }
}
