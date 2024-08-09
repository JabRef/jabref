package org.jabref.logic.ai;

import java.util.List;
import java.util.Map;

import org.jabref.preferences.ai.AiPreferences;
import org.jabref.preferences.ai.EmbeddingModel;

public class AiDefaultPreferences {
    public static final Map<AiPreferences.AiProvider, List<String>> AVAILABLE_CHAT_MODELS = Map.of(
            AiPreferences.AiProvider.OPEN_AI, List.of("gpt-4o-mini", "gpt-4o", "gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"),
            // "mistral" and "mixtral" are not language mistakes.
            AiPreferences.AiProvider.MISTRAL_AI, List.of("open-mistral-nemo", "open-mistral-7b", "open-mixtral-8x7b", "open-mixtral-8x22b", "mistral-large-latest"),
            AiPreferences.AiProvider.HUGGING_FACE, List.of()
    );

    public static final Map<AiPreferences.AiProvider, String> PROVIDERS_API_URLS = Map.of(
            AiPreferences.AiProvider.OPEN_AI, "https://api.openai.com/v1",
            AiPreferences.AiProvider.MISTRAL_AI, "https://api.mistral.ai/v1",
            AiPreferences.AiProvider.HUGGING_FACE, "https://huggingface.co/api"
    );

    public static final Map<AiPreferences.AiProvider, Map<String, Integer>> CONTEXT_WINDOW_SIZES = Map.of(
            AiPreferences.AiProvider.OPEN_AI, Map.of(
                    "gpt-4o-mini", 128000,
                    "gpt-4o", 128000,
                    "gpt-4", 8192,
                    "gpt-4-turbo", 128000,
                    "gpt-3.5-turbo", 16385
            ),
            AiPreferences.AiProvider.MISTRAL_AI, Map.of(
                    "mistral-large-latest", 128000,
                    "open-mistral-nemo", 128000,
                    "open-mistral-7b", 32000,
                    "open-mixtral-8x7b", 32000,
                    "open-mixtral-8x22b", 64000
            )
    );

    public static final boolean ENABLE_CHAT = false;

    public static final AiPreferences.AiProvider PROVIDER = AiPreferences.AiProvider.OPEN_AI;

    public static final Map<AiPreferences.AiProvider, String> CHAT_MODELS = Map.of(
            AiPreferences.AiProvider.OPEN_AI, "gpt-4o-mini",
            AiPreferences.AiProvider.MISTRAL_AI, "open-mixtral-8x22b",
            AiPreferences.AiProvider.HUGGING_FACE, ""
    );

    public static final boolean CUSTOMIZE_SETTINGS = false;

    public static final EmbeddingModel EMBEDDING_MODEL = EmbeddingModel.SENTENCE_TRANSFORMERS_ALL_MINILM_L12_V2;
    public static final String SYSTEM_MESSAGE = "You are an AI assistant that analyses research papers.";
    public static final double TEMPERATURE = 0.7;
    public static final int DOCUMENT_SPLITTER_CHUNK_SIZE = 300;
    public static final int DOCUMENT_SPLITTER_OVERLAP = 100;
    public static final int RAG_MAX_RESULTS_COUNT = 10;
    public static final double RAG_MIN_SCORE = 0.3;

    public static final int CONTEXT_WINDOW_SIZE = 8196;

    public static int getContextWindowSize(AiPreferences.AiProvider aiProvider, String model) {
        return CONTEXT_WINDOW_SIZES.getOrDefault(aiProvider, Map.of()).getOrDefault(model, 0);
    }
}
