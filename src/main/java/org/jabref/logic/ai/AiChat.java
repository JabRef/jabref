package org.jabref.logic.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.filter.Filter;
import org.jabref.preferences.AiPreferences;

/**
 * This class maintains an AI chat.
 * It depends on the {@link AiService}, which holds embedding and chat models.
 * This class was created for hiding the langchain4j state and listening to parameters change.
 * <p>
 * This class doesn't record the chat history.
 */
public class AiChat {
    private final AiService aiService;
    private final AiPreferences aiPreferences;

    private final Filter filter;

    // The main class that executes user prompts. Maintains API calls and retrieval augmented generation (RAG).
    private ConversationalRetrievalChain chain;

    // This class is also an "algorithm class" that maintains the chat history.
    // An algorithm for managing chat history is needed because you cannot stuff the whole history for the AI:
    // there would be too many tokens. This class, for example, sends only the 10 last messages.
    private ChatMemory chatMemory;

    public AiChat(AiService aiService, AiPreferences aiPreferences, Filter filter) {
        this.aiService = aiService;
        this.aiPreferences = aiPreferences;
        this.filter = filter;

        rebuild();

        listenToPreferences(aiService, aiPreferences);
    }

    private void listenToPreferences(AiService aiService, AiPreferences aiPreferences) {
        EasyBind.listen(aiService.chatModelProperty(), (obs, oldValue, newValue) -> {
            if (newValue != null) {
                rebuild();
            }
        });

        EasyBind.listen(aiService.embeddingModelProperty(), (obs, oldValue, newValue) -> {
            if (newValue != null) {
                rebuild();
            }
        });

        EasyBind.listen(aiPreferences.systemMessageProperty(), (obs, oldValue, newValue) -> {
            rebuild();
        });

        EasyBind.listen(aiPreferences.messageWindowSizeProperty(), (obs, oldValue, newValue) -> {
            rebuild();
        });

        EasyBind.listen(aiPreferences.documentSplitterChunkSizeProperty(), (obs, oldValue, newValue) -> {
            rebuild();
        });

        EasyBind.listen(aiPreferences.documentSplitterOverlapSizeProperty(), (obs, oldValue, newValue) -> {
            rebuild();
        });

        EasyBind.listen(aiPreferences.ragMinScoreProperty(), (obs, oldValue, newValue) -> {
            rebuild();
        });

        EasyBind.listen(aiPreferences.ragMaxResultsCountProperty(), (obs, oldValue, newValue) -> {
            rebuild();
        });
    }

    private void rebuild() {
        // When the user turns off the AI features all AiChat classes should be destroyed.
        // So this assert should never fail.
        assert aiService.getChatModel() != null;

        List<dev.langchain4j.data.message.ChatMessage> oldMessages = new ArrayList<>();
        if (chatMemory != null) {
            oldMessages = chatMemory.messages();
        }

        this.chatMemory = MessageWindowChatMemory
                .builder()
                .maxMessages(aiPreferences.getMessageWindowSize())
                .build();

        oldMessages.forEach(message -> chatMemory.add(message));

        // This class is basically an "algorithm class" for retrieving the relevant contents of documents.
        ContentRetriever contentRetirever = EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(aiService.getEmbeddingStore())
                .filter(filter)
                .embeddingModel(aiService.getEmbeddingModel())
                .maxResults(aiPreferences.getRagMaxResultsCount())
                .minScore(aiPreferences.getRagMinScore())
                .build();

        this.chain = ConversationalRetrievalChain
                .builder()
                .chatLanguageModel(aiService.getChatModel())
                .contentRetriever(contentRetirever)
                .chatMemory(chatMemory)
                .build();

        if (aiPreferences.getSystemMessage() != null || !aiPreferences.getSystemMessage().isEmpty()) {
            this.chatMemory.add(new SystemMessage(aiPreferences.getSystemMessage()));
        }
    }

    public String execute(String prompt) {
        // chain.execute() will automatically add messages to ChatMemory.
        return chain.execute(prompt);
    }

    public void restoreMessages(Stream<ChatMessage> messages) {
        messages.map(ChatMessage::toLangchainMessage).filter(Optional::isPresent).map(Optional::get).forEach(this.chatMemory::add);
    }
}
