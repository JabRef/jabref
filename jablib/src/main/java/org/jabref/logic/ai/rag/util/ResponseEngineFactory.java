package org.jabref.logic.ai.rag.util;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.EmbeddingsSearchResponseEngine;
import org.jabref.logic.ai.rag.logic.FullDocumentResponseEngine;
import org.jabref.logic.ai.rag.logic.ResponseEngine;
import org.jabref.model.ai.pipeline.ResponseEngineKind;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public final class ResponseEngineFactory {
    private ResponseEngineFactory() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static ResponseEngine create(
            ResponseEngineKind kind,
            FilePreferences filePreferences,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            double ragMinScore,
            int ragMaxResultsCount
    ) {
        return switch (kind) {
            case FULL_DOCUMENT ->
                    new FullDocumentResponseEngine(filePreferences);
            case EMBEDDINGS_SEARCH ->
                    new EmbeddingsSearchResponseEngine(
                            filePreferences,
                            embeddingModel,
                            embeddingStore,
                            ragMinScore,
                            ragMaxResultsCount
                    );
        };
    }

    public static ResponseEngine create(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        return create(
                aiPreferences.getResponseEngineKind(),
                filePreferences,
                embeddingModel,
                embeddingStore,
                aiPreferences.getRagMinScore(),
                aiPreferences.getRagMaxResultsCount()
        );
    }
}

