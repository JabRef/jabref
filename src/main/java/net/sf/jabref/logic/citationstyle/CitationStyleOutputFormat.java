package net.sf.jabref.logic.citationstyle;


public enum CitationStyleOutputFormat {

    HTML("html"),
    TEXT("text"),
    ASCII_DOC("asciidoc"),
    FO("fo"),
    RTF("rtf");

    public final String format;

    CitationStyleOutputFormat(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return format;
    }

}
