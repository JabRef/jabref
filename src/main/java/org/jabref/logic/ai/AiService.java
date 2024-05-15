package org.jabref.logic.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

/**
 * This class maintains the connection to AI services.
 * This is a global state of AI, and {@link AiChat}'s use this class.
 * <p>
 * An outer class is responsible for synchronizing objects of this class with {@link org.jabref.preferences.AiPreferences} changes.
 */
public class AiService {
    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    private final ChatMemoryStore chatMemoryStore = new InMemoryChatMemoryStore();

    public AiService(String apiKey) {
        // Later this class can accepts different enums or other pieces of information in order
        // to construct different chat models.

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

    public ChatMemoryStore getChatMemoryStore() {
        return chatMemoryStore;
    }
}
