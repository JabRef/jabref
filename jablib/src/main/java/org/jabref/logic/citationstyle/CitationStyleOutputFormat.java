package org.jabref.logic.citationstyle;

import org.jabref.logic.os.OS;

public enum CitationStyleOutputFormat {

    HTML("html", OS.NEWLINE + "<br>" + OS.NEWLINE),
    TEXT("text", ""),
    MARKDOWN("markdown", "");

    private final String format;
    private final String lineSeparator;

    CitationStyleOutputFormat(String format, String lineSeparator) {
        this.format = format;
        this.lineSeparator = lineSeparator;
    }

    public String getFormat() {
        return format;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    @Override
    public String toString() {
        return format;
    }
}
