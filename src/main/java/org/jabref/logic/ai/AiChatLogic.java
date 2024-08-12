package org.jabref.logic.ai;

import java.util.List;

import org.jabref.logic.ai.chathistory.AiChatHistory;
import org.jabref.logic.ai.misc.ErrorMessage;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.CanonicalBibEntry;
import org.jabref.model.entry.LinkedFile;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatLogic {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatLogic.class);

    private final AiService aiService;
    private final AiChatHistory aiChatHistory;
    private final Filter embeddingsFilter;
    private final BibEntry entry;

    private ChatMemory chatMemory;
    private Chain<String, String> chain;

    public AiChatLogic(AiService aiService, AiChatHistory aiChatHistory, Filter embeddingsFilter, BibEntry entry) {
        this.aiService = aiService;
        this.aiChatHistory = aiChatHistory;
        this.embeddingsFilter = embeddingsFilter;
        this.entry = entry;

        setupListeningToPreferencesChanges();
        rebuildFull(aiChatHistory.getMessages());
    }

    public static AiChatLogic forBibEntry(AiService aiService, AiChatHistory aiChatHistory, BibEntry entry) {
        Filter filter = MetadataFilterBuilder
                .metadataKey(FileEmbeddingsManager.LINK_METADATA_KEY)
                .isIn(entry
                        .getFiles()
                        .stream()
                        .map(LinkedFile::getLink)
                        .toList()
                );

        if (entry.getCitationKey().isEmpty()) {
            LOGGER.error("AiChatLogic should not be derived from BibEntry with no citation key");
        }

        return new AiChatLogic(aiService, aiChatHistory, filter, entry);
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

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(aiService.getEmbeddingsManager().getEmbeddingsStore())
                .filter(embeddingsFilter)
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
        return systemMessage + "\n" + CanonicalBibEntry.getCanonicalRepresentation(entry);
    }

    public AiMessage execute(UserMessage message) {
        // Message will be automatically added to ChatMemory through ConversationalRetrievalChain.

        LOGGER.info("Sending message to AI provider ({}) for answering in entry {}: {}",
                AiDefaultPreferences.PROVIDERS_API_URLS.get(aiService.getPreferences().getAiProvider()),
                entry.getCitationKey().orElse("<no citation key>"),
                message.singleText());

        aiChatHistory.add(message);
        AiMessage result = new AiMessage(chain.execute(message.singleText()));
        aiChatHistory.add(result);

        LOGGER.debug("Message was answered by the AI provider for entry {}: {}", entry.getCitationKey().orElse("<no citation key>"), result.text());

        return result;
    }

    public AiChatHistory getChatHistory() {
        return aiChatHistory;
    }
}
