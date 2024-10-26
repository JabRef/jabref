package org.jabref.logic.filenameformatpatterns;

public record FilenameFormatPattern(String stringRepresentation) {
    public static final FilenameFormatPattern NULL_FileName_PATTERN = new FilenameFormatPattern("");
}
