package org.jabref.logic.ai;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;

/**
 * This class maintains an AI chat.
 * It depends on the AiConnection, which holds embedding and chat models.
 * This class was created for maintaining (hiding) the langchain4j state.
 * <p>
 * AiChatData can be used to restore the serialized state of a chat.
 * <p>
 * An outer class is responsible for synchronizing objects of this class with AiPreference changes.
 */
public class AiChat {
    // Stores the embeddings and the chat history.
    private final AiChatData data;
    // The main class that executes user prompts. Maintains API calls and retrieval augmented generation (RAG).
    private final ConversationalRetrievalChain chain;
    // TODO: It turns out ChatMemoryStore is a more global class than I though. Make it global.
    // Initially I thought it is local to every chat.
    // But no, ChatMemoryStore is a store for many different chats.
    // In current implementation I use it as I said it first: different ChatMemoryStore for different chats,
    // so I need ID for several algorithms.
    private final Object chatId;

    public AiChat(AiConnection aiConnection) {
        this.data = new AiChatData();

        // This class is basically an "algorithm class" for retrieving the relevant contents of documents.
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(data.getEmbeddingStore())
                .embeddingModel(aiConnection.getEmbeddingModel())
                .build();

        // This class is also an "algorithm class" that maintains the chat history.
        // An algorithm for managing chat history is needed because you cannot stuff the whole history for the AI:
        // there would be too many tokens. This class, for example, sends only the 10 last messages.
        ChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .chatMemoryStore(data.getChatMemoryStore())
                .maxMessages(10) // This was the default value in the original implementation.
                .build();

        this.chatId = chatMemory.id();

        // TODO: Investigate whether the ChatMemoryStore holds the whole chat history.
        // As I think, ChatMemory should only send 10 last messages to the AI provider, but not remove the last
        // messages from the store.

        this.chain = ConversationalRetrievalChain
                .builder()
                .chatLanguageModel(aiConnection.getChatModel())
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }

    public AiChat(AiChatData data, AiConnection aiConnection) {
        // Code duplication...

        this.data = data;

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(data.getEmbeddingStore())
                .embeddingModel(aiConnection.getEmbeddingModel())
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .chatMemoryStore(data.getChatMemoryStore())
                .maxMessages(10) // This was the default value in the original implementation.
                .build();

        this.chatId = chatMemory.id();

        this.chain = ConversationalRetrievalChain
                .builder()
                .chatLanguageModel(aiConnection.getChatModel())
                .contentRetriever(contentRetriever)
                .chatMemory(chatMemory)
                .build();
    }

    public String execute(String prompt) {
        return chain.execute(prompt);
    }

    public AiChatData getData() {
        return data;
    }

    public Object getChatId() {
        return chatId;
    }
}
