package org.jabref.logic.citationkeypattern;

public record FilenameFormatPattern(String stringRepresentation) {
    public static final FilenameFormatPattern NULL_CITATION_KEY_PATTERN = new FilenameFormatPattern("");
}
