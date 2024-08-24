package org.jabref.logic.ai.chatting;

import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.AiDefaultPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.ingestion.FileEmbeddingsManager;
import org.jabref.logic.ai.util.ErrorMessage;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.CanonicalBibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.ListUtil;
import org.jabref.preferences.ai.AiPreferences;

import dev.langchain4j.chain.Chain;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatLogic {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatLogic.class);

    private final AiService aiService;
    private final ObservableList<ChatMessage> chatHistory;
    private final ObservableList<BibEntry> entries;
    private final StringProperty name;

    private ChatMemory chatMemory;
    private Chain<String, String> chain;

    public AiChatLogic(AiService aiService, StringProperty name, ObservableList<ChatMessage> chatHistory, ObservableList<BibEntry> entries) {
        this.aiService = aiService;
        this.chatHistory = chatHistory;
        this.entries = entries;
        this.name = name;

        this.entries.addListener((ListChangeListener<BibEntry>) change -> rebuildChain());

        setupListeningToPreferencesChanges();
        rebuildFull(chatHistory);
    }

    private void setupListeningToPreferencesChanges() {
        AiPreferences aiPreferences = aiService.getPreferences();

        aiPreferences.instructionProperty().addListener(obs -> setSystemMessage(aiPreferences.getInstruction()));
        aiPreferences.contextWindowSizeProperty().addListener(obs -> rebuildFull(chatMemory.messages()));
    }

    private void rebuildFull(List<ChatMessage> chatMessages) {
        rebuildChatMemory(chatMessages);
        rebuildChain();
    }

    private void rebuildChatMemory(List<ChatMessage> chatMessages) {
        AiPreferences aiPreferences = aiService.getPreferences();

        this.chatMemory = TokenWindowChatMemory
                .builder()
                .maxTokens(aiPreferences.getContextWindowSize(), new OpenAiTokenizer())
                .build();

        chatMessages.stream().filter(chatMessage -> !(chatMessage instanceof ErrorMessage)).forEach(chatMemory::add);

        setSystemMessage(aiPreferences.getInstruction());
    }

    private void rebuildChain() {
        AiPreferences aiPreferences = aiService.getPreferences();

        List<LinkedFile> linkedFiles = ListUtil.getLinkedFiles(entries).toList();
        @Nullable Filter filter;

        if (linkedFiles.isEmpty()) {
            // You must not pass an empty list to langchain4j {@link IsIn} filter.
            filter = null;
        } else {
            filter = MetadataFilterBuilder
                    .metadataKey(FileEmbeddingsManager.LINK_METADATA_KEY)
                    .isIn(linkedFiles
                            .stream()
                            .map(LinkedFile::getLink)
                            .toList()
                    );
        }

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(aiService.getEmbeddingsManager().getEmbeddingsStore())
                .filter(filter)
                .embeddingModel(aiService.getEmbeddingModel())
                .maxResults(aiPreferences.getRagMaxResultsCount())
                .minScore(aiPreferences.getRagMinScore())
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor
                .builder()
                .contentRetriever(contentRetriever)
                .executor(aiService.getCachedThreadPool())
                .build();

        this.chain = ConversationalRetrievalChain
                .builder()
                .chatLanguageModel(aiService.getChatLanguageModel())
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(chatMemory)
                .build();
    }

    private void setSystemMessage(String systemMessage) {
        chatMemory.add(new SystemMessage(augmentSystemMessage(systemMessage)));
    }

    private String augmentSystemMessage(String systemMessage) {
        String entriesInfo = entries.stream().map(CanonicalBibEntry::getCanonicalRepresentation).collect(Collectors.joining("\n"));

        return systemMessage + "\n" + entriesInfo;
    }

    public AiMessage execute(UserMessage message) {
        // Message will be automatically added to ChatMemory through ConversationalRetrievalChain.

        LOGGER.info("Sending message to AI provider ({}) for answering in {}: {}",
                AiDefaultPreferences.PROVIDERS_API_URLS.get(aiService.getPreferences().getAiProvider()),
                name.get(),
                message.singleText());

        chatHistory.add(message);
        AiMessage result = new AiMessage(chain.execute(message.singleText()));
        chatHistory.add(result);

        LOGGER.debug("Message was answered by the AI provider for {}: {}", name.get(), result.text());

        return result;
    }

    public ObservableList<ChatMessage> getChatHistory() {
        return chatHistory;
    }
}
