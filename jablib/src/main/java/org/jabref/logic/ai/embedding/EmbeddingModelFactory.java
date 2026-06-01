package org.jabref.logic.ai.embedding;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.embeddings.PredefinedEmbeddingModel;

/// Static factory for creating {@link AsyncEmbeddingModel} instances via an {@link EmbeddingModelCache}.
/// Always use the cache so that only one instance (and one background download/load task) is ever
/// created per {@link PredefinedEmbeddingModel}.
public final class EmbeddingModelFactory {
    private EmbeddingModelFactory() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /// Returns the cached {@link AsyncEmbeddingModel} for the given kind,
    /// creating it on first access so that only one background task is ever launched per kind.
    public static AsyncEmbeddingModel create(
            PredefinedEmbeddingModel embeddingModelKind,
            EmbeddingModelCache cache
    ) {
        return cache.getOrCreate(embeddingModelKind);
    }

    /// Convenience overload that reads the embedding model kind from {@link AiPreferences}.
    public static AsyncEmbeddingModel create(
            AiPreferences aiPreferences,
            EmbeddingModelCache cache
    ) {
        return cache.getOrCreate(aiPreferences.getEmbeddingModel());
    }
}
