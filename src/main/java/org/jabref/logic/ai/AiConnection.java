package org.jabref.logic.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * This class maintains the connection to AI services.
 * This is a global state of AI, and AiChat's use this class.
 * <p>
 * An outer class is responsible for synchronizing objects of this class with AiPreference changes.
 */
public class AiConnection {
    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    // Later this class can accepts different enums or other pices of information in order
    // to construct different chat models.
    public AiConnection(String apiKey) {
        this.chatModel = OpenAiChatModel
                .builder()
                .apiKey(apiKey)
                .build();
    }

    public ChatLanguageModel getChatModel() {
        return chatModel;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
}
