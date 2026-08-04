package org.jabref.logic.ocr;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum EngineSelection {
    OCRMYPDF("OCRmyPDF"),
    DOCLING("Docling");

    private final String displayName;

    EngineSelection(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static EngineSelection safeValueOf(String name) {
        try {
            return EngineSelection.valueOf(name);
        } catch (IllegalArgumentException e) {
            return EngineSelection.OCRMYPDF;
        }
    }
}
