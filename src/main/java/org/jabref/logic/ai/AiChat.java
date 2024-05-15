package org.jabref.logic.ai;

import java.util.UUID;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * This class maintains an AI chat.
 * It depends on the {@link AiService}, which holds embedding and chat models.
 * This class was created for maintaining (hiding) the langchain4j state.
 * <p>
 * An outer class is responsible for synchronizing objects of this class with {@link org.jabref.preferences.AiPreferences} changes.
 */
public class AiChat {
    public static final int MESSAGE_WINDOW_SIZE = 10;

    // The main class that executes user prompts. Maintains API calls and retrieval augmented generation (RAG).
    private final ConversationalRetrievalChain chain;

    private final Object chatId;

    public AiChat(AiService aiService, EmbeddingStore<TextSegment> embeddingStore) {
        // This class is basically an "algorithm class" for retrieving the relevant contents of documents.
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(aiService.getEmbeddingModel())
                .build();

        // This class is also an "algorithm class" that maintains the chat history.
        // An algorithm for managing chat history is needed because you cannot stuff the whole history for the AI:
        // there would be too many tokens. This class, for example, sends only the 10 last messages.
        ChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .chatMemoryStore(aiService.getChatMemoryStore())
                .maxMessages(MESSAGE_WINDOW_SIZE) // This was the default value in the original implementation.
                .id(UUID.randomUUID())
                .build();

        this.chatId = chatMemory.id();

        this.chain = ConversationalRetrievalChain
                .builder()
                .chatLanguageModel(aiService.getChatModel())
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }

    public String execute(String prompt) {
        return chain.execute(prompt);
    }

    public Object getChatId() {
        return chatId;
    }
}
