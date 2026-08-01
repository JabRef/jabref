package org.jabref.logic.ocr;

public enum EngineSelection {
    OCRMYPDF("OcrmyPDF"),
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
