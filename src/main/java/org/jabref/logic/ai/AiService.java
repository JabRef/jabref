package org.jabref.logic.ai;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.chat.AiChatLanguageModel;
import org.jabref.logic.ai.chathistory.AiChatHistoryManager;
import org.jabref.logic.ai.embeddings.AiEmbeddingModel;
import org.jabref.logic.ai.embeddings.AiEmbeddingsManager;
import org.jabref.preferences.AiPreferences;

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

    private final AiPreferences aiPreferences;

    private final AiChatLanguageModel aiChatLanguageModel;
    private final AiChatHistoryManager aiChatHistoryManager;

    private final AiEmbeddingModel aiEmbeddingModel;
    private final AiEmbeddingsManager aiEmbeddingsManager;

    public AiService(AiPreferences aiPreferences, DialogService dialogService) {
        this.aiPreferences = aiPreferences;
        this.aiChatLanguageModel = new AiChatLanguageModel(aiPreferences);
        this.aiChatHistoryManager = new AiChatHistoryManager(dialogService);
        this.aiEmbeddingModel = new AiEmbeddingModel(aiPreferences);
        this.aiEmbeddingsManager = new AiEmbeddingsManager(aiPreferences, dialogService);
    }

    @Override
    public void close() throws Exception {
        this.aiChatLanguageModel.close();
        this.aiChatHistoryManager.close();
        this.aiEmbeddingsManager.close();
    }

    public AiPreferences getPreferences() {
        return aiPreferences;
    }

    public AiChatLanguageModel getChatLanguageModel() {
        return aiChatLanguageModel;
    }

    public AiEmbeddingModel getEmbeddingModel() {
        return aiEmbeddingModel;
    }

    public AiChatHistoryManager getChatHistoryManager() {
        return aiChatHistoryManager;
    }

    public AiEmbeddingsManager getEmbeddingsManager() {
        return aiEmbeddingsManager;
    }
}
