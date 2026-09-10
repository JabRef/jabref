package org.jabref.model.ai.pipeline;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record RelevantInformation(
        @Nullable String source,
        @Nullable Integer pageNumber,
        String text
) {
}

