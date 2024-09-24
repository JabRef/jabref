package org.jabref.logic.ai;

import java.util.List;
import java.util.Map;

import org.jabref.model.ai.AiProvider;
import org.jabref.model.ai.EmbeddingModel;

public class AiDefaultPreferences {
    public static final Map<AiProvider, List<String>> AVAILABLE_CHAT_MODELS = Map.of(
            AiProvider.OPEN_AI, List.of("gpt-4o-mini", "gpt-4o", "gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"),
            // "mistral" and "mixtral" are not language mistakes.
            AiProvider.MISTRAL_AI, List.of("open-mistral-nemo", "open-mistral-7b", "open-mixtral-8x7b", "open-mixtral-8x22b", "mistral-large-latest"),
            AiProvider.GEMINI, List.of("gemini-1.5-flash", "gemini-1.5-pro", "gemini-1.0-pro"),
            AiProvider.HUGGING_FACE, List.of()
    );

    public static final Map<AiProvider, String> PROVIDERS_PRIVACY_POLICIES = Map.of(
            AiProvider.OPEN_AI, "https://openai.com/policies/privacy-policy/",
            AiProvider.MISTRAL_AI, "https://mistral.ai/terms/#privacy-policy",
            AiProvider.GEMINI, "https://ai.google.dev/gemini-api/terms",
            AiProvider.HUGGING_FACE, "https://huggingface.co/privacy"
    );

    public static final Map<AiProvider, String> PROVIDERS_API_URLS = Map.of(
            AiProvider.OPEN_AI, "https://api.openai.com/v1",
            AiProvider.MISTRAL_AI, "https://api.mistral.ai/v1",
            AiProvider.GEMINI, "https://generativelanguage.googleapis.com/v1beta/",
            AiProvider.HUGGING_FACE, "https://huggingface.co/api"
    );

    public static final Map<AiProvider, Map<String, Integer>> CONTEXT_WINDOW_SIZES = Map.of(
            AiProvider.OPEN_AI, Map.of(
                    "gpt-4o-mini", 128000,
                    "gpt-4o", 128000,
                    "gpt-4", 8192,
                    "gpt-4-turbo", 128000,
                    "gpt-3.5-turbo", 16385
            ),
            AiProvider.MISTRAL_AI, Map.of(
                    "mistral-large-latest", 128000,
                    "open-mistral-nemo", 128000,
                    "open-mistral-7b", 32000,
                    "open-mixtral-8x7b", 32000,
                    "open-mixtral-8x22b", 64000
            ),
            AiProvider.GEMINI, Map.of(
                    "gemini-1.5-flash", 1048576,
                    "gemini-1.5-pro", 2097152,
                    "gemini-1.0-pro", 32000
            )
    );

    public static final boolean ENABLE_CHAT = false;

    public static final AiProvider PROVIDER = AiProvider.OPEN_AI;

    public static final Map<AiProvider, String> CHAT_MODELS = Map.of(
            AiProvider.OPEN_AI, "gpt-4o-mini",
            AiProvider.MISTRAL_AI, "open-mixtral-8x22b",
            AiProvider.GEMINI, "gemini-1.5-flash",
            AiProvider.HUGGING_FACE, ""
    );

    public static final boolean CUSTOMIZE_SETTINGS = false;

    public static final EmbeddingModel EMBEDDING_MODEL = EmbeddingModel.SENTENCE_TRANSFORMERS_ALL_MINILM_L12_V2;
    public static final String SYSTEM_MESSAGE = "You are an AI assistant that analyses research papers. You answer questions about papers. You will be supplied with the necessary information. The supplied information will contain mentions of papers in form '@citationKey'. Whenever you refer to a paper, use its citation key in the same form with @ symbol. Whenever you find relevant information, always use the citation key. Here are the papers you are analyzing:\n";
    public static final double TEMPERATURE = 0.7;
    public static final int DOCUMENT_SPLITTER_CHUNK_SIZE = 300;
    public static final int DOCUMENT_SPLITTER_OVERLAP = 100;
    public static final int RAG_MAX_RESULTS_COUNT = 10;
    public static final double RAG_MIN_SCORE = 0.3;

    public static final int CONTEXT_WINDOW_SIZE = 8196;

    public static int getContextWindowSize(AiProvider aiProvider, String model) {
        return CONTEXT_WINDOW_SIZES.getOrDefault(aiProvider, Map.of()).getOrDefault(model, 0);
    }
}
