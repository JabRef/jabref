package org.jabref.logic.ai.chatting;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public class AiChatService {
    private final AiPreferences aiPreferences;
    private final ChatModel chatLanguageModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AiTemplatesService aiTemplatesService;

    public AiChatService(AiPreferences aiPreferences,
                         ChatModel chatLanguageModel,
                         EmbeddingModel embeddingModel,
                         EmbeddingStore<TextSegment> embeddingStore,
                         AiTemplatesService aiTemplatesService
    ) {
        this.aiPreferences = aiPreferences;
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.aiTemplatesService = aiTemplatesService;
    }

    public AiChatLogic makeChat(
            StringProperty name,
            ObservableList<ChatMessage> chatHistory,
            ObservableList<BibEntry> entries,
            BibDatabaseContext bibDatabaseContext
    ) {
        FollowUpQuestionGenerator followUpQuestionGenerator = new FollowUpQuestionGenerator(
                chatLanguageModel,
                aiTemplatesService,
                aiPreferences.getFollowUpQuestionsCount());
        return new AiChatLogic(
                aiPreferences,
                chatLanguageModel,
                embeddingModel,
                embeddingStore,
                aiTemplatesService,
                followUpQuestionGenerator,
                name,
                chatHistory,
                entries,
                bibDatabaseContext
        );
    }
}
