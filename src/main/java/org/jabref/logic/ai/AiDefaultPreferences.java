package org.jabref.logic.ai;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jabref.model.ai.AiProvider;
import org.jabref.model.ai.EmbeddingModel;

public class AiDefaultPreferences {
    public enum PredefinedChatModel {
        GPT_4O_MINI(AiProvider.OPEN_AI, "gpt-4o-mini", 128000),
        GPT_4O(AiProvider.OPEN_AI, "gpt-4o", 128000),
        GPT_4(AiProvider.OPEN_AI, "gpt-4", 8192),
        GPT_4_TURBO(AiProvider.OPEN_AI, "gpt-4-turbo", 128000),
        GPT_3_5_TURBO(AiProvider.OPEN_AI, "gpt-3.5-turbo", 16385),
        OPEN_MISTRAL_NEMO(AiProvider.MISTRAL_AI, "open-mistral-nemo", 128000),
        OPEN_MISTRAL_7B(AiProvider.MISTRAL_AI, "open-mistral-7b", 32000),
        // "mixtral" is not a typo.
        OPEN_MIXTRAL_8X7B(AiProvider.MISTRAL_AI, "open-mixtral-8x7b", 32000),
        OPEN_MIXTRAL_8X22B(AiProvider.MISTRAL_AI, "open-mixtral-8x22b", 64000),
        GEMINI_1_5_FLASH(AiProvider.GEMINI, "gemini-1.5-flash", 1048576),
        GEMINI_1_5_PRO(AiProvider.GEMINI, "gemini-1.5-pro", 2097152),
        GEMINI_1_0_PRO(AiProvider.GEMINI, "gemini-1.0-pro", 32000),
        // Dummy variant for Hugging Face models.
        HUGGING_FACE(AiProvider.HUGGING_FACE, "", 0);

        private final AiProvider aiProvider;
        private final String name;
        private final int contextWindowSize;

        PredefinedChatModel(AiProvider aiProvider, String name, int contextWindowSize) {
            this.aiProvider = aiProvider;
            this.name = name;
            this.contextWindowSize = contextWindowSize;
        }

        public AiProvider getAiProvider() {
            return aiProvider;
        }

        public String getName() {
            return name;
        }

        public int getContextWindowSize() {
            return contextWindowSize;
        }

        public String toString() {
            return aiProvider.toString() + " " + name;
        }
    }

    public static final boolean ENABLE_CHAT = false;
    public static final boolean AUTO_GENERATE_EMBEDDINGS = false;
    public static final boolean AUTO_GENERATE_SUMMARIES = false;

    public static final AiProvider PROVIDER = AiProvider.OPEN_AI;

    public static final Map<AiProvider, PredefinedChatModel> CHAT_MODELS = Map.of(
            AiProvider.OPEN_AI, PredefinedChatModel.GPT_4O_MINI,
            AiProvider.MISTRAL_AI, PredefinedChatModel.OPEN_MIXTRAL_8X22B,
            AiProvider.GEMINI, PredefinedChatModel.GEMINI_1_5_FLASH,
            AiProvider.HUGGING_FACE, PredefinedChatModel.HUGGING_FACE
    );

    public static final boolean CUSTOMIZE_SETTINGS = false;

    public static final EmbeddingModel EMBEDDING_MODEL = EmbeddingModel.SENTENCE_TRANSFORMERS_ALL_MINILM_L12_V2;
    public static final String SYSTEM_MESSAGE = "You are an AI assistant that analyses research papers. You answer questions about papers. You will be supplied with the necessary information. The supplied information will contain mentions of papers in form '@citationKey'. Whenever you refer to a paper, use its citation key in the same form with @ symbol. Whenever you find relevant information, always use the citation key. Here are the papers you are analyzing:\n";
    public static final double TEMPERATURE = 0.7;
    public static final int DOCUMENT_SPLITTER_CHUNK_SIZE = 300;
    public static final int DOCUMENT_SPLITTER_OVERLAP = 100;
    public static final int RAG_MAX_RESULTS_COUNT = 10;
    public static final double RAG_MIN_SCORE = 0.3;

    public static final int FALLBACK_CONTEXT_WINDOW_SIZE = 8196;

    public static List<String> getAvailableModels(AiProvider aiProvider) {
        return Arrays.stream(AiDefaultPreferences.PredefinedChatModel.values())
                     .filter(model -> model.getAiProvider() == aiProvider)
                     .map(AiDefaultPreferences.PredefinedChatModel::getName)
                     .toList();
    }

    public static int getContextWindowSize(AiProvider aiProvider, String modelName) {
        return Arrays.stream(AiDefaultPreferences.PredefinedChatModel.values())
                     .filter(model -> model.getAiProvider() == aiProvider && model.getName().equals(modelName))
                     .map(AiDefaultPreferences.PredefinedChatModel::getContextWindowSize)
                     .findFirst()
                     .orElse(AiDefaultPreferences.FALLBACK_CONTEXT_WINDOW_SIZE);
    }
}
