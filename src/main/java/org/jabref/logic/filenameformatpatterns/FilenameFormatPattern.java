package org.jabref.logic.filenameformatpatterns;

public record FilenameFormatPattern(String stringRepresentation) {
    public static final FilenameFormatPattern NULL_CITATION_KEY_PATTERN = new FilenameFormatPattern("");
}
