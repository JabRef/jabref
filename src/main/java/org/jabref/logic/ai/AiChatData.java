package org.jabref.logic.ai;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

/**
 * This class holds model agnostic information about one AI chat for a bib entry.
 * It stores all the embeddings, generated from the full-text article in linked files of a bib entry,
 * and the chat history.
 * <p>
 * This class could be used to serialize and deserialize the information for chat and/or recreate AiChat objects.
 * <p>
 * You may ask: why embeddings are stored in this class? Indeed, logically for one bib entry there could be many chats.
 * But in current implementation of JabRef there is only one chat per bib entry.
 * So in this class embeddings are also stored for convenience.
 */
public class AiChatData {
    // It's important to notice that all of these fields have an interface type, so we can easily create the specific
    // classes that we want in JabRef.

    // TODO: Investigate whether the embeddings generated from different models are compatible.
    // I guess not, so we have to invalidate and regenerate the embeddings if the model is changed.
    // Of course, that is the week 2, not week 1.
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatMemoryStore chatMemoryStore;

    public AiChatData() {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        this.chatMemoryStore = new InMemoryChatMemoryStore();
    }

    public AiChatData(EmbeddingStore<TextSegment> embeddingStore, ChatMemoryStore chatMemoryStore) {
        this.embeddingStore = embeddingStore;
        this.chatMemoryStore = chatMemoryStore;
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    public ChatMemoryStore getChatMemoryStore() {
        return chatMemoryStore;
    }
}
