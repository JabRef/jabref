package org.jabref.logic.ai;

import org.jabref.logic.ai.chat.AiChatLanguageModel;
import org.jabref.logic.ai.chathistory.ChatHistoryManager;
import org.jabref.logic.ai.embeddings.AiEmbeddingModel;
import org.jabref.logic.ai.embeddings.AiEmbeddingsManager;
import org.jabref.preferences.AiPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiService {
    public static final String VERSION = "1";

    private final Logger LOGGER = LoggerFactory.getLogger(AiService.class);

    private final AiPreferences aiPreferences;

    private final AiChatLanguageModel aiChatLanguageModel;
    private final ChatHistoryManager chatHistoryManager = new ChatHistoryManager();

    private final AiEmbeddingModel aiEmbeddingModel;
    private final AiEmbeddingsManager aiEmbeddingsManager;

    public AiService(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        this.aiChatLanguageModel = new AiChatLanguageModel(aiPreferences);
        this.aiEmbeddingModel = new AiEmbeddingModel(aiPreferences);
        this.aiEmbeddingsManager = new AiEmbeddingsManager();
    }

    public void close() {
        this.chatHistoryManager.close();
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

    public ChatHistoryManager getChatHistoryManager() {
        return chatHistoryManager;
    }

    public AiEmbeddingsManager getEmbeddingsManager() {
        return aiEmbeddingsManager;
    }
}
