package org.jabref.logic.citationkeypattern;

public record CitationKeyPattern(String stringRepresentation) {
    public static final CitationKeyPattern NULL_CITATION_KEY_PATTERN = new CitationKeyPattern("");
}
