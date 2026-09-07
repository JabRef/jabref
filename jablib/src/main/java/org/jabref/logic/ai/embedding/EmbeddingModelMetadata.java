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
        String size = downloadSizeBytes.isPresent()
                ? FileUtils.byteCountToDisplaySize(downloadSizeBytes.orElseThrow())
                : "";
        String tokenSuffix = maxSnippetTokens.isPresent()
                ? "(max " + maxSnippetTokens.orElseThrow() + " tokens)"
                : "";
        if (!size.isEmpty() && !tokenSuffix.isEmpty()) {
            return size + " " + tokenSuffix;
        }
        return size.isEmpty() ? tokenSuffix : size;
    }
}
