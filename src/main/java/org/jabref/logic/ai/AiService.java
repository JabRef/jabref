package org.jabref.logic.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.FilePreferences;
import org.jabref.logic.ai.chatting.AiChatService;
import org.jabref.logic.ai.chatting.model.JabRefChatLanguageModel;
import org.jabref.logic.ai.ingestion.IngestionService;
import org.jabref.logic.ai.ingestion.MVStoreEmbeddingStore;
import org.jabref.logic.ai.ingestion.model.JabRefEmbeddingModel;
import org.jabref.logic.ai.ingestion.storages.MVStoreFullyIngestedDocumentsTracker;
import org.jabref.logic.ai.summarization.SummariesService;
import org.jabref.logic.ai.summarization.storages.MVStoreSummariesStorage;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.NotificationService;
import org.jabref.logic.util.TaskExecutor;

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

    // This field is used to shut down AI-related background tasks.
    // If a background task processes a big document and has a loop, then the task should check the status
    // of this property for being true. If it's true, then it should abort the cycle.
    private final BooleanProperty shutdownSignal = new SimpleBooleanProperty(false);

    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-retrieval-pool-%d").build()
    );

    private final MVStoreEmbeddingStore mvStoreEmbeddingStore;
    private final MVStoreFullyIngestedDocumentsTracker mvStoreFullyIngestedDocumentsTracker;
    private final MVStoreSummariesStorage mvStoreSummariesStorage;

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
        this.jabRefChatLanguageModel = new JabRefChatLanguageModel(aiPreferences);

        this.mvStoreEmbeddingStore = new MVStoreEmbeddingStore(Directories.getAiFilesDirectory().resolve(EMBEDDINGS_FILE_NAME), notificationService);
        this.mvStoreFullyIngestedDocumentsTracker = new MVStoreFullyIngestedDocumentsTracker(Directories.getAiFilesDirectory().resolve(FULLY_INGESTED_FILE_NAME), notificationService);
        this.mvStoreSummariesStorage = new MVStoreSummariesStorage(Directories.getAiFilesDirectory().resolve(SUMMARIES_FILE_NAME), notificationService);

        this.jabRefEmbeddingModel = new JabRefEmbeddingModel(aiPreferences, notificationService, taskExecutor);
        this.aiChatService = new AiChatService(aiPreferences, jabRefChatLanguageModel, jabRefEmbeddingModel, mvStoreEmbeddingStore, cachedThreadPool);
        this.ingestionService = new IngestionService(
                aiPreferences,
                shutdownSignal,
                jabRefEmbeddingModel,
                mvStoreEmbeddingStore,
                mvStoreFullyIngestedDocumentsTracker,
                filePreferences,
                taskExecutor
        );
        this.summariesService = new SummariesService(aiPreferences, mvStoreSummariesStorage, jabRefChatLanguageModel, shutdownSignal, filePreferences, taskExecutor);
    }

    public JabRefChatLanguageModel getChatLanguageModel() {
        return jabRefChatLanguageModel;
    }

    public JabRefEmbeddingModel getEmbeddingModel() {
        return jabRefEmbeddingModel;
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
