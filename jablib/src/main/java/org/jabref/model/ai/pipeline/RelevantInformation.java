package org.jabref.model.ai.pipeline;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record RelevantInformation(
        @Nullable String source,
        @Nullable Integer pageNumber,
        String text
) {
    @JsonCreator
    public RelevantInformation(
            @JsonProperty("source") @Nullable String source,
            @JsonProperty("pageNumber") @Nullable Integer pageNumber,
            @JsonProperty("text") String text
    ) {
        this.source = source;
        this.pageNumber = pageNumber;
        this.text = text;
    }

    public RelevantInformation(@Nullable String source, String text) {
        this(source, null, text);
    }

    public RelevantInformation(@Nullable String source, String text, @Nullable Integer pageNumber) {
        this(source, pageNumber, text);
    }

    public @Nullable String citationKey() {
        return source;
    }
}

