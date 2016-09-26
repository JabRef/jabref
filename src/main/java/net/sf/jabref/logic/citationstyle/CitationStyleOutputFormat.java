package net.sf.jabref.logic.citationstyle;


public enum CitationStyleOutputFormat {

    HTML("html"),
    TEXT("text"),
    ASCII_DOC("asciidoc"),
    FO("fo"),
    RTF("rtf");

    private final String format;

    CitationStyleOutputFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public String toString() {
        return format;
    }

}
