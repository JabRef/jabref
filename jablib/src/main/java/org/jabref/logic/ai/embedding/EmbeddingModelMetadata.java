package org.jabref.logic.ai.embedding;

import java.util.OptionalInt;
import java.util.OptionalLong;

import org.apache.commons.io.FileUtils;
import org.jspecify.annotations.NullMarked;

/// Metadata for an AI embedding model, including its model name, download size,
/// and maximum input snippet / sequence length in tokens.
@NullMarked
public record EmbeddingModelMetadata(
        String modelName,
        OptionalLong downloadSizeBytes,
        OptionalInt maxSnippetTokens
) {
    public String sizeInfo() {
        return downloadSizeBytes.isPresent()
                ? FileUtils.byteCountToDisplaySize(downloadSizeBytes.orElseThrow())
                : "";
    }

    public String displayLabel() {
        String sizePrefix = downloadSizeBytes.isPresent() ? "[" + sizeInfo() + "] " : "";
        String tokenSuffix = maxSnippetTokens.isPresent() ? " (max " + maxSnippetTokens.orElseThrow() + " tokens)" : "";
        return sizePrefix + modelName + tokenSuffix;
    }
}
