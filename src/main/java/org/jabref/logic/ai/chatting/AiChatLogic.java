package org.jabref.logic.ai.chatting;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.AiDefaultPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.ingestion.FileEmbeddingsManager;
import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.logic.ai.templates.PaperExcerpt;
import org.jabref.logic.ai.templates.TemplatesService;
import org.jabref.logic.ai.util.ErrorMessage;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.CanonicalBibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.ListUtil;

import ai.djl.training.optimizer.Optimizer;
import dev.langchain4j.chain.Chain;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatLogic {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatLogic.class);

    private final AiPreferences aiPreferences;
    private final ChatLanguageModel chatLanguageModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final TemplatesService templatesService;

    private final ObservableList<ChatMessage> chatHistory;
    private final ObservableList<BibEntry> entries;
    private final StringProperty name;
    private final BibDatabaseContext bibDatabaseContext;

    private ChatMemory chatMemory;

    // You must not pass an empty list to langchain4j {@link IsIn} filter. We forced to use null here, instead of Optional.
    @Nullable Filter filter;

    public AiChatLogic(AiPreferences aiPreferences,
                       ChatLanguageModel chatLanguageModel,
                       EmbeddingModel embeddingModel,
                       EmbeddingStore<TextSegment> embeddingStore,
                       TemplatesService templatesService,
                       StringProperty name,
                       ObservableList<ChatMessage> chatHistory,
                       ObservableList<BibEntry> entries,
                       BibDatabaseContext bibDatabaseContext
    ) {
        this.aiPreferences = aiPreferences;
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.templatesService = templatesService;
        this.chatHistory = chatHistory;
        this.entries = entries;
        this.name = name;
        this.bibDatabaseContext = bibDatabaseContext;

        this.entries.addListener((ListChangeListener<BibEntry>) change -> rebuildFilter());

        setupListeningToPreferencesChanges();
        rebuildFull(chatHistory);
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.getTemplates().addListener((MapChangeListener<AiTemplate, String>) change -> {
            if (change.getKey() == AiTemplate.CHATTING_SYSTEM_MESSAGE) {
                setSystemMessage(templatesService.makeChattingSystemMessage(entries));
            }
        });

        aiPreferences.contextWindowSizeProperty().addListener(obs -> rebuildFull(chatMemory.messages()));
    }

    private void rebuildFull(List<ChatMessage> chatMessages) {
        rebuildChatMemory(chatMessages);
        rebuildFilter();
    }

    private void rebuildChatMemory(List<ChatMessage> chatMessages) {
        this.chatMemory = TokenWindowChatMemory
                .builder()
                .maxTokens(aiPreferences.getContextWindowSize(), new OpenAiTokenizer())
                .build();

        chatMessages.stream().filter(chatMessage -> !(chatMessage instanceof ErrorMessage)).forEach(chatMemory::add);

        setSystemMessage(templatesService.makeChattingSystemMessage(entries));
    }

    private void rebuildFilter() {
        List<LinkedFile> linkedFiles = ListUtil.getLinkedFiles(entries).toList();

        if (linkedFiles.isEmpty()) {
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

        chatHistory.add(message);

        LOGGER.info("Sending message to AI provider ({}) for answering in {}: {}",
                AiDefaultPreferences.PROVIDERS_API_URLS.get(aiPreferences.getAiProvider()),
                name.get(),
                message.singleText());

        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest
                .builder()
                .maxResults(aiPreferences.getRagMaxResultsCount())
                .minScore(aiPreferences.getRagMinScore())
                .filter(filter)
                .queryEmbedding(embeddingModel.embed(message.singleText()).content())
                .build();

        EmbeddingSearchResult<TextSegment> embeddingSearchResult = embeddingStore.search(embeddingSearchRequest);

        List<PaperExcerpt> excerpts = embeddingSearchResult
                .matches()
                .stream()
                .map(EmbeddingMatch::embedded)
                .map(textSegment -> {
                    String link = textSegment.metadata().getString(FileEmbeddingsManager.LINK_METADATA_KEY);

                    if (link == null) {
                        return new PaperExcerpt("", textSegment.text());
                    } else {
                        return new PaperExcerpt(findEntryByLink(link).flatMap(BibEntry::getCitationKey).orElse(""), textSegment.text());
                    }
                })
                .toList();

        LOGGER.debug("Found excerpts for the message: {}", excerpts);

        // This is crazy, but langchain4j {@link ChatMemory} does not allow to remove single messages.
        ChatMemory tempChatMemory = TokenWindowChatMemory
                .builder()
                .maxTokens(aiPreferences.getContextWindowSize(), new OpenAiTokenizer())
                .build();

        chatMemory.messages().forEach(tempChatMemory::add);

        tempChatMemory.add(new UserMessage(templatesService.makeChattingUserMessage(message.singleText(), excerpts)));
        chatMemory.add(message);

        AiMessage aiMessage = chatLanguageModel.generate(tempChatMemory.messages()).content();

        chatMemory.add(aiMessage);
        chatHistory.add(aiMessage);

        LOGGER.debug("Message was answered by the AI provider for {}: {}", name.get(), aiMessage.text());

        return aiMessage;
    }

    private Optional<BibEntry> findEntryByLink(String link) {
        return bibDatabaseContext
                .getEntries()
                .stream()
                .filter(entry -> entry.getFiles().stream().anyMatch(file -> file.getLink().equals(link)))
                .findFirst();
    }

    public ObservableList<ChatMessage> getChatHistory() {
        return chatHistory;
    }
}
