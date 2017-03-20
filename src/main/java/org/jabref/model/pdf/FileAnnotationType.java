package org.jabref.model.pdf;


public enum FileAnnotationType {
    TEXT("Text"),
    HIGHLIGHT("Highlight"),
    UNDERLINE("Underline"),
    POLYGON("Polygon"),
    POPUP("Popup"),
    LINE("Line"),
    CIRCLE("Circle"),
    FREETEXT("FreeText"),
    STRIKEOUT("Strikeout"),
    LINK("Link"),
    NONE("None");

    private final String name;

    FileAnnotationType(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}
