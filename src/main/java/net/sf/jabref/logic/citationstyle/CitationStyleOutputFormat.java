package net.sf.jabref.logic.citationstyle;


public enum CitationStyleOutputFormat {

    ASCII_DOC("asciidoc"),
    HTML("html"),
    RTF("rtf"),
    TEXT("text"),
    XSLFO("fo");

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
