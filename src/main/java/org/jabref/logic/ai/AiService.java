package org.jabref.logic.ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.chathistory.BibDatabaseChatHistoryManager;
import org.jabref.logic.ai.models.JabRefChatLanguageModel;
import org.jabref.logic.ai.models.JabRefEmbeddingModel;
import org.jabref.logic.ai.summarization.SummariesStorage;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.ai.AiPreferences;

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

    // This field is used to shut down AI-related background tasks.
    // If a background task processes a big document and has a loop, then the task should check the status
    // of this property for being true. If it's true, then it should abort the cycle.
    private final BooleanProperty shutdownSignal = new SimpleBooleanProperty(false);

    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-retrieval-pool-%d").build()
    );

    private final MVStore mvStore;

    private final JabRefChatLanguageModel jabRefChatLanguageModel;
    private final BibDatabaseChatHistoryManager bibDatabaseChatHistoryManager;

    private final JabRefEmbeddingModel jabRefEmbeddingModel;
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
            LOGGER.error("An error occurred while creating directories for AI cache and chat history. Chat history will not be remembered in next session", e);
            dialogService.notify(Localization.lang("An error occurred while creating directories for AI cache and chat history. Chat history will not be remembered in next session"));
            mvStore = MVStore.open(null);
        }

        this.mvStore = mvStore;

        this.jabRefChatLanguageModel = new JabRefChatLanguageModel(aiPreferences);
        this.bibDatabaseChatHistoryManager = new BibDatabaseChatHistoryManager(mvStore);
        this.jabRefEmbeddingModel = new JabRefEmbeddingModel(aiPreferences, dialogService, taskExecutor);
        this.fileEmbeddingsManager = new FileEmbeddingsManager(aiPreferences, shutdownSignal, jabRefEmbeddingModel, mvStore);
        this.summariesStorage = new SummariesStorage(aiPreferences, mvStore);
    }

    @Override
    public void close() {
        shutdownSignal.set(true);

        this.cachedThreadPool.shutdownNow();
        this.jabRefChatLanguageModel.close();
        this.jabRefEmbeddingModel.close();
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

    public JabRefEmbeddingModel getEmbeddingModel() {
        return jabRefEmbeddingModel;
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

    public ReadOnlyBooleanProperty getShutdownSignal() {
        return shutdownSignal;
    }
}
