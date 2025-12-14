package org.jabref.model.citation;

import java.util.Objects;
import java.util.Optional;

public record CitationContext(
        String citationMarker,
        String contextText,
        String sourceCitationKey,
        Optional<Integer> pageNumber
) {
    public CitationContext {
        Objects.requireNonNull(citationMarker, "Citation marker cannot be null");
        Objects.requireNonNull(contextText, "Context text cannot be null");
        Objects.requireNonNull(sourceCitationKey, "Source citation key cannot be null");
        Objects.requireNonNull(pageNumber, "Page number optional cannot be null");

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
