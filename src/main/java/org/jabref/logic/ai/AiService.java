package org.jabref.logic.ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.chathistory.BibDatabaseChatHistoryManager;
import org.jabref.logic.ai.models.EmbeddingModel;
import org.jabref.logic.ai.models.JabRefChatLanguageModel;
import org.jabref.logic.ai.summarization.SummariesStorage;
import org.jabref.preferences.AiPreferences;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  The main class for the AI functionality.
 *  <p>
 *  Holds all the AI components: LLM and embedding model, chat history and embeddings cache.
 */
public class AiService implements AutoCloseable {
    public static final String VERSION = "1";

    private static final Logger LOGGER = LoggerFactory.getLogger(AiService.class);
    private static final String AI_SERVICE_MVSTORE_FILE_NAME = "ai.mv";

    private final AiPreferences aiPreferences;
    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-retrieval-pool-%d").build()
    );

    private final MVStore mvStore;

    private final JabRefChatLanguageModel jabRefChatLanguageModel;
    private final BibDatabaseChatHistoryManager bibDatabaseChatHistoryManager;

    private final EmbeddingModel embeddingModel;
    private final FileEmbeddingsManager fileEmbeddingsManager;

    private final SummariesStorage summariesStorage;

    public AiService(AiPreferences aiPreferences, DialogService dialogService, TaskExecutor taskExecutor) {
        this.aiPreferences = aiPreferences;

        MVStore mvStore;
        try {
            Files.createDirectories(JabRefDesktop.getAiFilesDirectory());

            Path mvStorePath = JabRefDesktop.getAiFilesDirectory().resolve(AI_SERVICE_MVSTORE_FILE_NAME);

            mvStore = MVStore.open(mvStorePath.toString());
        } catch (Exception e) {
            LOGGER.error("An error occurred while creating directories for storing chat history. Chat history won't be remembered in next session", e);
            dialogService.notify("An error occurred while creating directories for storing chat history. Chat history won't be remembered in next session");
            mvStore = MVStore.open(null);
        }

        this.mvStore = mvStore;

        this.jabRefChatLanguageModel = new JabRefChatLanguageModel(aiPreferences);
        this.bibDatabaseChatHistoryManager = new BibDatabaseChatHistoryManager(mvStore);
        this.embeddingModel = new EmbeddingModel(aiPreferences, dialogService, taskExecutor);
        this.fileEmbeddingsManager = new FileEmbeddingsManager(aiPreferences, embeddingModel, mvStore);
        this.summariesStorage = new SummariesStorage(mvStore);
    }

    @Override
    public void close() {
        this.cachedThreadPool.shutdownNow();
        this.jabRefChatLanguageModel.close();
        this.embeddingModel.close();
        this.bibDatabaseChatHistoryManager.close();
        this.mvStore.close();
    }

    public AiPreferences getPreferences() {
        return aiPreferences;
    }

    public ExecutorService getCachedThreadPool() {
        return cachedThreadPool;
    }

    public JabRefChatLanguageModel getChatLanguageModel() {
        return jabRefChatLanguageModel;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public BibDatabaseChatHistoryManager getChatHistoryManager() {
        return bibDatabaseChatHistoryManager;
    }

    public FileEmbeddingsManager getEmbeddingsManager() {
        return fileEmbeddingsManager;
    }

    public SummariesStorage getSummariesStorage() {
        return summariesStorage;
    }
}
