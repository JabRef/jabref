package org.jabref.logic.ai.rag.util;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.rag.logic.AnswerEngine;
import org.jabref.logic.ai.rag.logic.EmbeddingsSearchAnswerEngine;
import org.jabref.logic.ai.rag.logic.FullDocumentAnswerEngine;
import org.jabref.model.ai.pipeline.AnswerEngineKind;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

public final class AnswerEngineFactory {
    private AnswerEngineFactory() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static AnswerEngine create(
            AnswerEngineKind kind,
            FilePreferences filePreferences,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore,
            double ragMinScore,
            int ragMaxResultsCount
    ) {
        return switch (kind) {
            case FULL_DOCUMENT ->
                    new FullDocumentAnswerEngine(filePreferences);
            case EMBEDDINGS_SEARCH ->
                    new EmbeddingsSearchAnswerEngine(
                            filePreferences,
                            embeddingModel,
                            embeddingStore,
                            ragMinScore,
                            ragMaxResultsCount
                    );
        };
    }

    public static AnswerEngine create(
            AiPreferences aiPreferences,
            FilePreferences filePreferences,
            EmbeddingModel embeddingModel,
            EmbeddingStore<TextSegment> embeddingStore
    ) {
        return create(
                aiPreferences.getAnswerEngineKind(),
                filePreferences,
                embeddingModel,
                embeddingStore,
                aiPreferences.getRagMinScore(),
                aiPreferences.getRagMaxResultsCount()
        );
    }
}
