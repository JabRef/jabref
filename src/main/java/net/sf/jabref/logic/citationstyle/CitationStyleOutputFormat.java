package net.sf.jabref.logic.citationstyle;


import net.sf.jabref.logic.util.OS;

public enum CitationStyleOutputFormat {

    ASCII_DOC("asciidoc", OS.NEWLINE),
    HTML("html", OS.NEWLINE + "<br>" + OS.NEWLINE),
    RTF("rtf", "\\line" + OS.NEWLINE),
    TEXT("text", OS.NEWLINE),
    XSL_FO("fo", OS.NEWLINE);

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
