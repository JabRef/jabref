package org.jabref.logic.ai.ingestion.util;

import org.jabref.logic.ai.ingestion.logic.documentsplitting.DocumentSplitter;
import org.jabref.logic.ai.ingestion.logic.documentsplitting.SlidingWindowDocumentSplitter;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.model.ai.pipeline.DocumentSplitterKind;

/// Static factory for creating {@link DocumentSplitter} instances.
public final class DocumentSplitterFactory {
    private DocumentSplitterFactory() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    public static DocumentSplitter create(DocumentSplitterKind kind, int chunkSize, int overlapSize) {
        return switch (kind) {
            case SLIDING_WINDOW ->
                    new SlidingWindowDocumentSplitter(chunkSize, overlapSize);
        };
    }

    public static DocumentSplitter create(AiPreferences aiPreferences) {
        return create(
                aiPreferences.getDocumentSplitterKind(),
                aiPreferences.getDocumentSplitterChunkSize(),
                aiPreferences.getDocumentSplitterOverlapSize()
        );
    }
}
