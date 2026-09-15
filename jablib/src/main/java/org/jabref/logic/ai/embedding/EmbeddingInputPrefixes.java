package org.jabref.logic.ai.embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.jspecify.annotations.NullMarked;

/// E5 models are trained with `query: ` and `passage: ` prefixes and retrieve worse without them,
/// see <https://huggingface.co/intfloat/e5-small-v2#faq>. Other models get the text unchanged.
@NullMarked
public final class EmbeddingInputPrefixes {
    private EmbeddingInputPrefixes() {
    }

    public static String forQuery(EmbeddingModel embeddingModel, String text) {
        return isE5(embeddingModel) ? "query: " + text : text;
    }

    public static String forPassage(EmbeddingModel embeddingModel, String text) {
        return isE5(embeddingModel) ? "passage: " + text : text;
    }

    static boolean isE5(String modelName) {
        // Instruct variants use a different prompt format
        return modelName.startsWith("intfloat/") && modelName.contains("e5-") && !modelName.contains("instruct");
    }

    private static boolean isE5(EmbeddingModel embeddingModel) {
        return embeddingModel instanceof AsyncEmbeddingModel asyncEmbeddingModel && isE5(asyncEmbeddingModel.getModelName());
    }
}
