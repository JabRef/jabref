package org.jabref.logic.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.chathistory.BibDatabaseChatHistoryManager;
import org.jabref.logic.ai.models.JabRefChatLanguageModel;
import org.jabref.logic.ai.models.EmbeddingModel;
import org.jabref.preferences.AiPreferences;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 *  The main class for the AI functionality.
 *  <p>
 *  Holds all the AI components: LLM and embedding model, chat history and embeddings cache.
 */
public class AiService implements AutoCloseable {
    public static final String VERSION = "1";

    private final AiPreferences aiPreferences;
    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-retrieval-pool-%d").build()
    );

    private final JabRefChatLanguageModel jabRefChatLanguageModel;
    private final BibDatabaseChatHistoryManager bibDatabaseChatHistoryManager;

    private final EmbeddingModel embeddingModel;
    private final FileEmbeddingsManager fileEmbeddingsManager;

    public AiService(AiPreferences aiPreferences, DialogService dialogService) {
        this.aiPreferences = aiPreferences;
        this.jabRefChatLanguageModel = new JabRefChatLanguageModel(aiPreferences);
        this.bibDatabaseChatHistoryManager = new BibDatabaseChatHistoryManager(dialogService);
        this.embeddingModel = new EmbeddingModel(aiPreferences);
        this.fileEmbeddingsManager = new FileEmbeddingsManager(aiPreferences, embeddingModel, dialogService);
    }

    @Override
    public void close() throws Exception {
        this.cachedThreadPool.shutdownNow();
        this.jabRefChatLanguageModel.close();
        this.embeddingModel.close();
        this.bibDatabaseChatHistoryManager.close();
        this.fileEmbeddingsManager.close();
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
}
