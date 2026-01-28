package org.jabref.logic.ai.chatting;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.ingestion.FileEmbeddingsManager;
import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.logic.ai.templates.AiTemplatesService;
import org.jabref.logic.ai.templates.PaperExcerpt;
import org.jabref.logic.ai.util.ErrorMessage;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.ListUtil;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatLogic {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatLogic.class);

    private final AiPreferences aiPreferences;
    private final ChatModel chatLanguageModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AiTemplatesService aiTemplatesService;
    private final FollowUpQuestionGenerator followUpQuestionGenerator;

    private final ObservableList<String> followUpQuestions = FXCollections.observableArrayList();
    private final ObservableList<ChatMessage> chatHistory;
    private final ObservableList<BibEntry> entries;
    private final StringProperty name;
    private final BibDatabaseContext bibDatabaseContext;

    private ChatMemory chatMemory;

    private Optional<Filter> filter = Optional.empty();

    public AiChatLogic(AiPreferences aiPreferences,
                       ChatModel chatLanguageModel,
                       EmbeddingModel embeddingModel,
                       EmbeddingStore<TextSegment> embeddingStore,
                       AiTemplatesService aiTemplatesService,
                       FollowUpQuestionGenerator followUpQuestionGenerator,
                       StringProperty name,
                       ObservableList<ChatMessage> chatHistory,
                       ObservableList<BibEntry> entries,
                       BibDatabaseContext bibDatabaseContext
    ) {
        this.aiPreferences = aiPreferences;
        this.chatLanguageModel = chatLanguageModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.aiTemplatesService = aiTemplatesService;
        this.followUpQuestionGenerator = followUpQuestionGenerator;
        this.chatHistory = chatHistory;
        this.entries = entries;
        this.name = name;
        this.bibDatabaseContext = bibDatabaseContext;

        this.entries.addListener((ListChangeListener<BibEntry>) change -> rebuildFilter());

        setupListeningToPreferencesChanges();
        rebuildFull(chatHistory);
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences
                .templateProperty(AiTemplate.CHATTING_SYSTEM_MESSAGE)
                .addListener(obs ->
                        setSystemMessage(aiTemplatesService.makeChattingSystemMessage(entries)));

        aiPreferences.contextWindowSizeProperty().addListener(obs -> rebuildFull(chatMemory.messages()));
    }

    private void rebuildFull(List<ChatMessage> chatMessages) {
        rebuildChatMemory(chatMessages);
        rebuildFilter();
    }

    private void rebuildChatMemory(List<ChatMessage> chatMessages) {
        // Because we can't get a tokenizer for each model, {@link AiChatLogic} assumes that
        // every text is tokenized like it's tokenized for OpenAI's GPT-4o-mini model.
        //
        // Reasons why we can't get tokenizer for each model:
        // - Some tokenizers might not be available in langchain4j.
        // - User may use a custom model, but there is no way to supply a custom tokenizer.
        // - OpenAI API (and compatible ones) doesn't have an endpoint for tokenizing text.
        //
        // This is another dark workaround of AI integration. But it works "good-enough" for now.
        this.chatMemory = TokenWindowChatMemory
                .builder()
                .maxTokens(aiPreferences.getContextWindowSize(), new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4_O_MINI))
                .build();

        chatMessages.stream().filter(chatMessage -> !(chatMessage instanceof ErrorMessage)).forEach(chatMemory::add);

        setSystemMessage(aiTemplatesService.makeChattingSystemMessage(entries));
    }

    private void rebuildFilter() {
        List<LinkedFile> linkedFiles = ListUtil.getLinkedFiles(entries).toList();

        if (linkedFiles.isEmpty()) {
            filter = Optional.empty();
        } else {
            filter = Optional.of(MetadataFilterBuilder
                    .metadataKey(FileEmbeddingsManager.LINK_METADATA_KEY)
                    .isIn(linkedFiles
                            .stream()
                            .map(LinkedFile::getLink)
                            .toList()
                    ));
        }
    }

    private void setSystemMessage(String systemMessage) {
        chatMemory.add(new SystemMessage(systemMessage));
    }

    public AiMessage execute(UserMessage message) {
        // Message will be automatically added to ChatMemory through ConversationalRetrievalChain.

        chatHistory.add(message);

        LOGGER.info("Sending message to AI provider ({}) for answering in {}: {}",
                aiPreferences.getAiProvider().getApiUrl(),
                name.get(),
                message.singleText());

        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest
                .builder()
                .maxResults(aiPreferences.getRagMaxResultsCount())
                .minScore(aiPreferences.getRagMinScore())
                .filter(filter.orElse(null))
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
                .maxTokens(aiPreferences.getContextWindowSize(), new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4_O_MINI))
                .build();

        chatMemory.messages().forEach(tempChatMemory::add);

        tempChatMemory.add(new UserMessage(aiTemplatesService.makeChattingUserMessage(entries, message.singleText(), excerpts)));
        chatMemory.add(message);

        AiMessage aiMessage = chatLanguageModel.chat(tempChatMemory.messages()).aiMessage();

        chatMemory.add(aiMessage);
        chatHistory.add(aiMessage);

        LOGGER.debug("Message was answered by the AI provider for {}: {}", name.get(), aiMessage.text());

        if (aiPreferences.getGenerateFollowUpQuestions()) {
            try {
                List<String> questions = followUpQuestionGenerator.generateFollowUpQuestions(message, aiMessage);
                followUpQuestions.setAll(questions);
            } catch (Exception e) {
                LOGGER.warn("Failed to generate follow-up questions", e);
                followUpQuestions.clear();
            }
        } else {
            followUpQuestions.clear();
        }

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

    public ObservableList<String> getFollowUpQuestions() {
        return followUpQuestions;
    }
}
