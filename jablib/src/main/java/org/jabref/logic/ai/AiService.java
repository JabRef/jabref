package org.jabref.logic.ai;

import java.util.UUID;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.InMemoryChatHistoryCache;
import org.jabref.logic.ai.chatting.migrations.ChatHistoryMigrationV1;
import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.ai.chatting.repositories.MVStoreChatHistoryRepository;
import org.jabref.logic.ai.chatting.util.ChatModelFactory;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.embedding.EmbeddingModelFactory;
import org.jabref.logic.ai.embedding.MVStoreEmbeddingStore;
import org.jabref.logic.ai.ingestion.IngestionTaskAggregator;
import org.jabref.logic.ai.ingestion.listeners.GenerateEmbeddingsAiDatabaseListener;
import org.jabref.logic.ai.ingestion.logic.EmbeddingsCleaner;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.repositories.IngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.repositories.MVStoreIngestedDocumentsRepository;
import org.jabref.logic.ai.ingestion.util.DocumentSplitterFactory;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.InMemorySummaryCache;
import org.jabref.logic.ai.summarization.SummarizationTaskAggregator;
import org.jabref.logic.ai.summarization.listeners.GenerateSummaryAiDatabaseListener;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.migration.SummariesMigrationV1;
import org.jabref.logic.ai.summarization.repositories.MVStoreSummariesRepository;
import org.jabref.logic.ai.summarization.repositories.SummariesRepository;
import org.jabref.logic.ai.summarization.util.SummarizatorFactory;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.ObservablesHelper;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

/// The main class for the AI functionality.
/// Holds all the AI components: LLM and embedding model, chat history and embedding cache.
public class AiService implements AutoCloseable {
    public static final String VERSION = "2";

    private static final String CHAT_HISTORY_FILE_NAME = "chat-histories.mv";
    private static final String EMBEDDINGS_FILE_NAME = "embeddings.mv";
    private static final String FULLY_INGESTED_FILE_NAME = "fully-ingested.mv";
    private static final String SUMMARIES_FILE_NAME = "summaries.mv";

    private final NotificationService notificationService;

    // Chatting components
    private final MVStoreChatHistoryRepository mvStoreChatHistoryRepository;
    private final InMemoryChatHistoryCache inMemoryChatHistoryCache;
    private final ObjectProperty<ChatModel> currentChatModel = new SimpleObjectProperty<>();

    // Ingestion components
    private final EmbeddingModelCache embeddingModelCache;
    private final MVStoreEmbeddingStore mvStoreEmbeddingStore;
    private final MVStoreIngestedDocumentsRepository mvStoreIngestedDocumentsRepository;
    private final IngestionTaskAggregator ingestionTaskAggregator;
    private final EmbeddingsCleaner embeddingsCleaner;
    private final GenerateEmbeddingsAiDatabaseListener generateEmbeddingsAiDatabaseListener;
    private final ObjectProperty<DocumentSplitter> currentDocumentSplitter = new SimpleObjectProperty<>();
    private final ObjectProperty<AsyncEmbeddingModel> currentEmbeddingModel = new SimpleObjectProperty<>();

    // Summarization components
    private final MVStoreSummariesRepository mvStoreSummariesRepository;
    private final InMemorySummaryCache inMemorySummaryCache;
    private final SummarizationTaskAggregator summarizationTaskAggregator;
    private final GenerateSummaryAiDatabaseListener generateSummaryAiDatabaseListener;
    private final ObjectProperty<Summarizator> currentSummarizator = new SimpleObjectProperty<>();

    public AiService(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            NotificationService notificationService,
            TaskExecutor taskExecutor
    ) {
        this.notificationService = notificationService;

        // Chatting components
        this.mvStoreChatHistoryRepository = new MVStoreChatHistoryRepository(
                Directories.getAiFilesDirectory().resolve(CHAT_HISTORY_FILE_NAME),
                notificationService
        );
        this.inMemoryChatHistoryCache = new InMemoryChatHistoryCache(mvStoreChatHistoryRepository);
        this.currentChatModel.bind(ObservablesHelper.createClosableObjectBinding(
                () -> ChatModelFactory.create(aiPreferences),
                aiPreferences.getChatProperties()
        ));

        // Ingestion components
        this.embeddingModelCache = new EmbeddingModelCache(notificationService, taskExecutor);
        this.mvStoreEmbeddingStore = new MVStoreEmbeddingStore(
                Directories.getAiFilesDirectory().resolve(EMBEDDINGS_FILE_NAME),
                notificationService
        );
        this.mvStoreIngestedDocumentsRepository = new MVStoreIngestedDocumentsRepository(
                notificationService,
                Directories.getAiFilesDirectory().resolve(FULLY_INGESTED_FILE_NAME)
        );
        this.ingestionTaskAggregator = new IngestionTaskAggregator(taskExecutor);
        this.embeddingsCleaner = new EmbeddingsCleaner(
                aiPreferences,
                mvStoreEmbeddingStore,
                mvStoreIngestedDocumentsRepository
        );
        this.generateEmbeddingsAiDatabaseListener = new GenerateEmbeddingsAiDatabaseListener(
                aiPreferences,
                filePreferences,
                mvStoreIngestedDocumentsRepository,
                mvStoreEmbeddingStore,
                embeddingModelCache,
                ingestionTaskAggregator
        );
        this.currentEmbeddingModel.bind(ObservablesHelper.createClosableObjectBinding(
                () -> EmbeddingModelFactory.create(aiPreferences, this.embeddingModelCache),
                aiPreferences.getEmbeddingsProperties()
        ));

        this.currentDocumentSplitter.bind(ObservablesHelper.createObjectBinding(
                () -> DocumentSplitterFactory.create(aiPreferences),
                aiPreferences.getDocumentSplitterProperties()
        ));

        // Summarization components
        this.mvStoreSummariesRepository = new MVStoreSummariesRepository(
                notificationService,
                Directories.getAiFilesDirectory().resolve(SUMMARIES_FILE_NAME)
        );
        this.inMemorySummaryCache = new InMemorySummaryCache(mvStoreSummariesRepository);
        this.summarizationTaskAggregator = new SummarizationTaskAggregator(taskExecutor, inMemorySummaryCache);
        this.generateSummaryAiDatabaseListener = new GenerateSummaryAiDatabaseListener(
                aiPreferences,
                filePreferences,
                summarizationTaskAggregator
        );
        this.currentSummarizator.bind(ObservablesHelper.createObjectBinding(
                () -> SummarizatorFactory.create(aiPreferences),
                aiPreferences.getSummarizatorProperties()
        ));
    }

    public void setupDatabase(BibDatabaseContext context, boolean isDummyContext) {
        generateEmbeddingsAiDatabaseListener.setupDatabase(context);
        generateSummaryAiDatabaseListener.setupDatabase(context);

        if (!isDummyContext) {
            ensureAiLibraryIdPresent(context);
            migrateDatabase(context);
        }
    }

    private void ensureAiLibraryIdPresent(BibDatabaseContext bibDatabaseContext) {
        if (bibDatabaseContext.getMetaData().getAiLibraryId().isEmpty()) {
            bibDatabaseContext.getMetaData().setEventPropagation(false);

            // Adding a `finally` block just in case an error occurs when calling `setLibraryId`.
            try {
                bibDatabaseContext.getMetaData().setAiLibraryId(UUID.randomUUID().toString());
            } finally {
                bibDatabaseContext.getMetaData().setEventPropagation(true);
            }
        }
    }

    private void migrateDatabase(BibDatabaseContext context) {
        ChatHistoryMigrationV1.migrate(
                context,
                mvStoreChatHistoryRepository,
                notificationService
        );

        SummariesMigrationV1.migrate(
                context,
                mvStoreSummariesRepository,
                notificationService
        );
    }

    public ChatHistoryRepository getChatHistoryRepository() {
        return mvStoreChatHistoryRepository;
    }

    public InMemoryChatHistoryCache getChatHistoryCache() {
        return inMemoryChatHistoryCache;
    }

    public ChatModel getCurrentChatModel() {
        return currentChatModel.get();
    }

    public EmbeddingModelCache getEmbeddingModelCache() {
        return embeddingModelCache;
    }

    public EmbeddingStore<TextSegment> getEmbeddingsStore() {
        return mvStoreEmbeddingStore;
    }

    public IngestedDocumentsRepository getIngestedDocumentsRepository() {
        return mvStoreIngestedDocumentsRepository;
    }

    public IngestionTaskAggregator getIngestionTaskAggregator() {
        return ingestionTaskAggregator;
    }

    public EmbeddingsCleaner getEmbeddingsCleaner() {
        return embeddingsCleaner;
    }

    public DocumentSplitter getCurrentDocumentSplitter() {
        return currentDocumentSplitter.get();
    }

    public AsyncEmbeddingModel getCurrentEmbeddingModel() {
        return currentEmbeddingModel.get();
    }

    public SummariesRepository getSummariesRepository() {
        return mvStoreSummariesRepository;
    }

    public InMemorySummaryCache getSummaryCache() {
        return inMemorySummaryCache;
    }

    public SummarizationTaskAggregator getSummarizationTaskAggregator() {
        return summarizationTaskAggregator;
    }

    public Summarizator getCurrentSummarizator() {
        return currentSummarizator.get();
    }

    @Override
    public void close() throws Exception {
        // Close listeners
        generateSummaryAiDatabaseListener.close();
        generateEmbeddingsAiDatabaseListener.close();

        // Flush caches to repositories (chat history handles smart transfers)
        inMemoryChatHistoryCache.close();
        inMemorySummaryCache.close();

        // Close embedding model cache
        embeddingModelCache.close();

        // Close repositories
        mvStoreSummariesRepository.close();
        mvStoreChatHistoryRepository.close();
        mvStoreEmbeddingStore.close();
        mvStoreIngestedDocumentsRepository.close();
    }
}
