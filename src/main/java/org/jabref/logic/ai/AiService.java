package org.jabref.logic.ai;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.chathistory.BibDatabaseChatHistoryManager;
import org.jabref.logic.ai.impl.models.ChatLanguageModel;
import org.jabref.logic.ai.impl.models.EmbeddingModel;
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

    private final ChatLanguageModel chatLanguageModel;
    private final BibDatabaseChatHistoryManager bibDatabaseChatHistoryManager;

    private final EmbeddingModel embeddingModel;
    private final AiEmbeddingsManager aiEmbeddingsManager;

    public AiService(AiPreferences aiPreferences, DialogService dialogService) {
        this.aiPreferences = aiPreferences;
        this.chatLanguageModel = new ChatLanguageModel(aiPreferences);
        this.bibDatabaseChatHistoryManager = new BibDatabaseChatHistoryManager(dialogService);
        this.embeddingModel = new EmbeddingModel(aiPreferences);
        this.aiEmbeddingsManager = new AiEmbeddingsManager(aiPreferences, embeddingModel, dialogService);
    }

    @Override
    public void close() throws Exception {
        this.cachedThreadPool.shutdownNow();
        this.chatLanguageModel.close();
        this.embeddingModel.close();
        this.bibDatabaseChatHistoryManager.close();
        this.aiEmbeddingsManager.close();
    }

    public AiPreferences getPreferences() {
        return aiPreferences;
    }

    public ExecutorService getCachedThreadPool() {
        return cachedThreadPool;
    }

    public ChatLanguageModel getChatLanguageModel() {
        return chatLanguageModel;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }

    public BibDatabaseChatHistoryManager getChatHistoryManager() {
        return bibDatabaseChatHistoryManager;
    }

    public AiEmbeddingsManager getEmbeddingsManager() {
        return aiEmbeddingsManager;
    }
}
