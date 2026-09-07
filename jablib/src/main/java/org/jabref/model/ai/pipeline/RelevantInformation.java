package org.jabref.model.ai.pipeline;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record RelevantInformation(
        @JsonAlias("source") @JsonProperty("citationKey") @Nullable String citationKey,
        @JsonProperty("text") String text
) {
    public @Nullable String source() {
        return citationKey;
    }

    public @Nullable String getSource() {
        return citationKey;
    }

    public @Nullable String getCitationKey() {
        return citationKey;
    }

    public String getText() {
        return text;
    }
}
