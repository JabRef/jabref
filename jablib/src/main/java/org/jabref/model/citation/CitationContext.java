package org.jabref.model.citation;

import java.util.Optional;

import org.jspecify.annotations.NonNull;

public record CitationContext(
        @NonNull String citationMarker,
        @NonNull String contextText,
        @NonNull String sourceCitationKey,
        @NonNull Optional<Integer> pageNumber
) {
    public CitationContext {
        if (citationMarker.isBlank()) {
            throw new IllegalArgumentException("Citation marker cannot be blank");
        }
    }

    public CitationContext(String citationMarker, String contextText, String sourceCitationKey) {
        this(citationMarker, contextText, sourceCitationKey, Optional.empty());
    }

    public CitationContext(String citationMarker, String contextText, String sourceCitationKey, int pageNumber) {
        this(citationMarker, contextText, sourceCitationKey, Optional.of(pageNumber));
    }

    public String formatForComment() {
        return "[%s]: %s".formatted(sourceCitationKey, contextText);
    }

    public String getNormalizedMarker() {
        return citationMarker
                .replaceAll("[\\[\\](){}]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
