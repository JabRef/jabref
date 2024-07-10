package org.jabref.logic.ai;

import org.jabref.preferences.AiPreferences;

public class AiDefaultPreferences {
    public static final boolean ENABLE_CHAT = false;
    public static final boolean CUSTOMIZE_SETTINGS = false;
    public static final AiPreferences.ChatModel CHAT_MODEL = AiPreferences.ChatModel.GPT_3_5_TURBO;
    public static final AiPreferences.EmbeddingModel EMBEDDING_MODEL = AiPreferences.EmbeddingModel.ALL_MINLM_l6_V2;
    public static final String SYSTEM_MESSAGE = "You are an AI assistant that analyses research papers.";
    public static final double TEMPERATURE = 0.7;
    public static final int MESSAGE_WINDOW_SIZE = 1_000; // "2 + 2 = 4, 3 + 3 = 6" has size 20. Source: {@link dev.langchain4j.memory.chat.TokenWindowChatMemoryTest#should_evict_multiple_orphan_ToolExecutionResultMessages_when_evicting_AiMessage_with_ToolExecutionRequests_when_SystemMessage_is_present}
    public static final int DOCUMENT_SPLITTER_CHUNK_SIZE = 300;
    public static final int DOCUMENT_SPLITTER_OVERLAP = 100;
    public static final int RAG_MAX_RESULTS_COUNT = 10;
    public static final double RAG_MIN_SCORE = 0.3;
}
