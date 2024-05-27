package org.jabref.logic.ai;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.common.eventbus.Subscribe;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;

import org.jabref.logic.ai.events.ChatModelChangedEvent;
import org.jabref.model.entry.BibEntry;

/**
 * This class maintains an AI chat.
 * It depends on the {@link AiService}, which holds embedding and chat models.
 * This class was created for maintaining (hiding) the langchain4j state.
 * <p>
 * An outer class is responsible for synchronizing objects of this class with {@link org.jabref.preferences.AiPreferences} changes.
 */
public class AiChat {
    public static final int MESSAGE_WINDOW_SIZE = 10;
    public static final int RAG_MAX_RESULTS = 10;
    public static final double RAG_MIN_SCORE = 0.5;

    private final AiService aiService;
    private final Filter filter;

    // The main class that executes user prompts. Maintains API calls and retrieval augmented generation (RAG).
    private ConversationalRetrievalChain chain;

    private final ChatMemory chatMemory;

    public AiChat(AiService aiService, Filter filter) {
        this.aiService = aiService;
        this.filter = filter;

        // This class is also an "algorithm class" that maintains the chat history.
        // An algorithm for managing chat history is needed because you cannot stuff the whole history for the AI:
        // there would be too many tokens. This class, for example, sends only the 10 last messages.
        this.chatMemory = MessageWindowChatMemory
                .builder()
                .maxMessages(MESSAGE_WINDOW_SIZE) // This was the default value in the original implementation.
                .build();

        // When the user turns off the AI features all AiChat classes should be destroyed.
        // So this assert should never fail.
        assert aiService.getChatModel() != null;

        this.chain = ConversationalRetrievalChain
                .builder()
                .chatLanguageModel(aiService.getChatModel())
                .contentRetriever(makeContentRetriever())
                .chatMemory(chatMemory)
                .build();

        aiService.registerListener(this);
    }

    private ContentRetriever makeContentRetriever() {
        // This class is basically an "algorithm class" for retrieving the relevant contents of documents.
        return EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(aiService.getEmbeddingStore())
                .filter(filter)
                .embeddingModel(aiService.getEmbeddingModel())
                .maxResults(RAG_MAX_RESULTS)
                .minScore(RAG_MIN_SCORE)
                .build();
    }

    @Subscribe
    public void listen(ChatModelChangedEvent event) {
        if (event.getChatModel() != null) {
            this.chain = ConversationalRetrievalChain
                    .builder()
                    .chatLanguageModel(event.getChatModel())
                    .contentRetriever(makeContentRetriever())
                    .chatMemory(chatMemory)
                    .build();
        }
    }

    public void setSystemMessage(String message) {
        // ChatMemory automatically manages that there is only one system message.
        this.chatMemory.add(new SystemMessage(message));
    }

    public String execute(String prompt) {
        // chain.execute() will automatically add messages to ChatMemory.
        return chain.execute(prompt);
    }

    public void restoreMessages(List<ChatMessage> messages) {
        messages.stream().map(ChatMessage::toLangchainMessage).filter(Optional::isPresent).map(Optional::get).forEach(this.chatMemory::add);
    }
}
