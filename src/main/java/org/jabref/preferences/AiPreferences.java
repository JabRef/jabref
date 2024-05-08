package org.jabref.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class AiPreferences {
    private final ObjectProperty<EmbeddingModel> embeddingModel;

    public AiPreferences(EmbeddingModel embeddingModel, ChatLanguageModel chatModel) {
        this.embeddingModel = new SimpleObjectProperty<>(embeddingModel);
        this.chatModel = new SimpleObjectProperty<>(chatModel);
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel.get();
    }

    public ObjectProperty<EmbeddingModel> embeddingModelProperty() {
        return embeddingModel;
    }

    public void setEmbeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel.set(embeddingModel);
    }

    private final ObjectProperty<ChatLanguageModel> chatModel;

    public ChatLanguageModel getChatModel() {
        return chatModel.get();
    }

    public ObjectProperty<ChatLanguageModel> chatModelProperty() {
        return chatModel;
    }

    public void setChatModel(ChatLanguageModel chatModel) {
        this.chatModel.set(chatModel);
    }
}
