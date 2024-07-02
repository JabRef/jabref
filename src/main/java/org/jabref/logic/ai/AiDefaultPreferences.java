package org.jabref.logic.ai;

import org.jabref.preferences.AiPreferences;

public class AiDefaultPreferences {
    public static final boolean ENABLE_CHAT = false;

    public static final AiPreferences.AiProvider PROVIDER = AiPreferences.AiProvider.OPEN_AI;
    public static final String CHAT_MODEL = AiPreferences.CHAT_MODELS.get(PROVIDER).getFirst();

    public static final boolean CUSTOMIZE_SETTINGS = false;

    public static final AiPreferences.EmbeddingModel EMBEDDING_MODEL = AiPreferences.EmbeddingModel.ALL_MINLM_l6_V2;
    public static final String SYSTEM_MESSAGE = "You are an AI assistant that analyses research papers.";
    public static final double TEMPERATURE = 0.7;
    public static final int MESSAGE_WINDOW_SIZE = 10;
    public static final int DOCUMENT_SPLITTER_CHUNK_SIZE = 300;
    public static final int DOCUMENT_SPLITTER_OVERLAP = 100;
    public static final int RAG_MAX_RESULTS_COUNT = 10;
    public static final double RAG_MIN_SCORE = 0.3;
}
