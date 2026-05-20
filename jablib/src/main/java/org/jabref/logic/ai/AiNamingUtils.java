package org.jabref.logic.ai;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.embeddings.EmbeddingSimilarityMetric;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.pipeline.AnswerEngineKind;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;
import org.jabref.model.ai.summarization.SummarizatorKind;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

public final class AiNamingUtils {
    private AiNamingUtils() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static String getDisplayName(AiProvider provider) {
        return switch (provider) {
            case OPEN_AI ->
                    Localization.lang("OpenAI (or API compatible)");
            case MISTRAL_AI ->
                    Localization.lang("Mistral AI");
            case GEMINI ->
                    Localization.lang("Gemini");
            case HUGGING_FACE ->
                    Localization.lang("Hugging Face");
        };
    }

    public static String getDisplayName(AnswerEngineKind kind) {
        return switch (kind) {
            case EMBEDDINGS_SEARCH ->
                    Localization.lang("Embeddings Search");
            case FULL_DOCUMENT ->
                    Localization.lang("Full Document");
        };
    }

    public static String getDisplayName(DocumentSplitterKind kind) {
        return switch (kind) {
            case SLIDING_WINDOW ->
                    Localization.lang("Sliding Window");
        };
    }

    public static String getDisplayName(SummarizatorKind kind) {
        return switch (kind) {
            case CHUNKED ->
                    Localization.lang("Chunked");
            case FULL_DOCUMENT ->
                    Localization.lang("Full Document");
        };
    }

    public static String getDisplayName(TokenEstimatorKind kind) {
        return switch (kind) {
            case AVERAGE ->
                    Localization.lang("Average");
            case WORDS ->
                    Localization.lang("Words");
            case CHARS ->
                    Localization.lang("Characters");
            case MAX ->
                    Localization.lang("Max");
            case MIN ->
                    Localization.lang("Min");
        };
    }

    public static String getDisplayName(EmbeddingSimilarityMetric metric) {
        return switch (metric) {
            case COSINE_SIMILARITY ->
                    Localization.lang("Cosine Similarity");
        };
    }

    public static String getDisplayName(ChatMessage.Role role) {
        return switch (role) {
            case SYSTEM ->
                    Localization.lang("System");
            case USER ->
                    Localization.lang("User");
            case AI ->
                    Localization.lang("AI");
            case ERROR ->
                    Localization.lang("Error");
        };
    }
}

