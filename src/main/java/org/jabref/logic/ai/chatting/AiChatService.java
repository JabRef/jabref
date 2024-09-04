package org.jabref.logic.ai.chatting;

import java.util.concurrent.Executor;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.ai.AiPreferences;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class AiChatService {
    private final AiPreferences aiPreferences;
    private final ChatLanguageModel chatLanguageModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final Executor cachedThreadPool;

    public AiChatService(AiPreferences aiPreferences,
                       ChatLanguageModel chatLanguageModel,
                       EmbeddingModel embeddingModel,
                       EmbeddingStore<TextSegment> embeddingStore,
                       Executor cachedThreadPool
    ) {
        this.aiPreferences = aiPreferences;
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.cachedThreadPool = cachedThreadPool;
    }

    public AiChatLogic makeChat(
            StringProperty name,
            ObservableList<ChatMessage> chatHistory,
            ObservableList<BibEntry> entries,
            BibDatabaseContext bibDatabaseContext
    ) {
        return new AiChatLogic(
                aiPreferences,
                chatLanguageModel,
                embeddingModel,
                embeddingStore,
                cachedThreadPool,
                name,
                chatHistory,
                entries,
                bibDatabaseContext
        );
    }
}
