package org.jabref.logic.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.AiChatService;
import org.jabref.logic.ai.chatting.ChatHistoryService;
import org.jabref.logic.ai.chatting.chathistory.storages.MVStoreChatHistoryStorage;
import org.jabref.logic.ai.chatting.model.JabRefChatLanguageModel;
import org.jabref.logic.ai.ingestion.IngestionService;
import org.jabref.logic.ai.ingestion.MVStoreEmbeddingStore;
import org.jabref.logic.ai.ingestion.model.JabRefEmbeddingModel;
import org.jabref.logic.ai.ingestion.storages.MVStoreFullyIngestedDocumentsTracker;
import org.jabref.logic.ai.summarization.SummariesService;
import org.jabref.logic.ai.summarization.storages.MVStoreSummariesStorage;
import org.jabref.logic.ai.templates.TemplatesService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 *  The main class for the AI functionality.
 *  <p>
 *  Holds all the AI components: LLM and embedding model, chat history and embeddings cache.
 */
public class AiService implements AutoCloseable {
    public static final String VERSION = "1";

    private static final String EMBEDDINGS_FILE_NAME = "embeddings.mv";
    private static final String FULLY_INGESTED_FILE_NAME = "fully-ingested.mv";
    private static final String SUMMARIES_FILE_NAME = "summaries.mv";
    private static final String CHAT_HISTORY_FILE_NAME = "chat-histories.mv";

    // This field is used to shut down AI-related background tasks.
    // If a background task processes a big document and has a loop, then the task should check the status
    // of this property for being true. If it's true, then it should abort the cycle.
    private final BooleanProperty shutdownSignal = new SimpleBooleanProperty(false);

    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-retrieval-pool-%d").build()
    );

    private final MVStoreChatHistoryStorage mvStoreChatHistoryStorage;
    private final MVStoreEmbeddingStore mvStoreEmbeddingStore;
    private final MVStoreFullyIngestedDocumentsTracker mvStoreFullyIngestedDocumentsTracker;
    private final MVStoreSummariesStorage mvStoreSummariesStorage;

    private final ChatHistoryService chatHistoryService;
    private final JabRefChatLanguageModel jabRefChatLanguageModel;
    private final JabRefEmbeddingModel jabRefEmbeddingModel;
    private final AiChatService aiChatService;
    private final IngestionService ingestionService;
    private final SummariesService summariesService;

    public AiService(AiPreferences aiPreferences,
                     FilePreferences filePreferences,
                     CitationKeyPatternPreferences citationKeyPatternPreferences,
                     NotificationService notificationService,
                     TaskExecutor taskExecutor
    ) {

        this.mvStoreChatHistoryStorage = new MVStoreChatHistoryStorage(Directories.getAiFilesDirectory().resolve(CHAT_HISTORY_FILE_NAME), notificationService);
        this.mvStoreEmbeddingStore = new MVStoreEmbeddingStore(Directories.getAiFilesDirectory().resolve(EMBEDDINGS_FILE_NAME), notificationService);
        this.mvStoreFullyIngestedDocumentsTracker = new MVStoreFullyIngestedDocumentsTracker(Directories.getAiFilesDirectory().resolve(FULLY_INGESTED_FILE_NAME), notificationService);
        this.mvStoreSummariesStorage = new MVStoreSummariesStorage(Directories.getAiFilesDirectory().resolve(SUMMARIES_FILE_NAME), notificationService);

        TemplatesService templatesService = new TemplatesService(aiPreferences);
        this.chatHistoryService = new ChatHistoryService(citationKeyPatternPreferences, mvStoreChatHistoryStorage);
        this.jabRefChatLanguageModel = new JabRefChatLanguageModel(aiPreferences);
        this.jabRefEmbeddingModel = new JabRefEmbeddingModel(aiPreferences, notificationService, taskExecutor);

        this.aiChatService = new AiChatService(aiPreferences, jabRefChatLanguageModel, jabRefEmbeddingModel, mvStoreEmbeddingStore, templatesService);

        this.ingestionService = new IngestionService(
                aiPreferences,
                shutdownSignal,
                jabRefEmbeddingModel,
                mvStoreEmbeddingStore,
                mvStoreFullyIngestedDocumentsTracker,
                filePreferences,
                taskExecutor
        );

        this.summariesService = new SummariesService(
                aiPreferences,
                mvStoreSummariesStorage,
                jabRefChatLanguageModel,
                templatesService,
                shutdownSignal,
                filePreferences,
                taskExecutor
        );
    }

    public JabRefChatLanguageModel getChatLanguageModel() {
        return jabRefChatLanguageModel;
    }

    public JabRefEmbeddingModel getEmbeddingModel() {
        return jabRefEmbeddingModel;
    }

    public ChatHistoryService getChatHistoryService() {
        return chatHistoryService;
    }

    public AiChatService getAiChatService() {
        return aiChatService;
    }

    public IngestionService getIngestionService() {
        return ingestionService;
    }

    public SummariesService getSummariesService() {
        return summariesService;
    }

    public void setupDatabase(BibDatabaseContext context) {
        chatHistoryService.setupDatabase(context);
        ingestionService.setupDatabase(context);
        summariesService.setupDatabase(context);
    }

    @Override
    public void close() {
        shutdownSignal.set(true);

        cachedThreadPool.shutdownNow();
        jabRefChatLanguageModel.close();
        jabRefEmbeddingModel.close();

        mvStoreFullyIngestedDocumentsTracker.close();
        mvStoreEmbeddingStore.close();
        mvStoreSummariesStorage.close();
    }
}
