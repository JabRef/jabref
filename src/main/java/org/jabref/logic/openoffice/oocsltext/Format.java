package org.jabref.logic.openoffice.oocsltext;

public enum Format {
    TITLE("Title"),
    BODY_TEXT("Body Text"),
    SUBTITLE("Subtitle"),
    HEADING_1("Heading 1"),
    HEADING_2("Heading 2"),
    HEADING_3("Heading 3"),
    HEADING_4("Heading 4"),
    HEADING("Heading");

    private final String format;

    Format(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }
}
