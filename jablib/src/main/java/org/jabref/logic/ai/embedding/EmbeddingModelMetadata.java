package org.jabref.logic.ai.embedding;

import java.util.OptionalInt;
import java.util.OptionalLong;

import org.jspecify.annotations.NullMarked;

/// Metadata for an AI embedding model, including its model name, download size,
/// and maximum input snippet / sequence length in tokens.
@NullMarked
public record EmbeddingModelMetadata(
        String modelName,
        OptionalLong downloadSizeBytes,
        OptionalInt maxSnippetTokens
) {
}
