package org.jabref.logic.ocr;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum EngineSelection {
    OCRMYPDF("OCRmyPDF", Optional.empty()),
    EASYOCR("EasyOCR", Optional.of("ocrmypdf_easyocr")),
    DOCLING("Docling", Optional.empty());

    private final String displayName;
    private final Optional<String> identifierName;

    EngineSelection(String displayName, Optional<String> identifierName) {
        this.displayName = displayName;
        this.identifierName = identifierName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Optional<String> getIdentifierName() {
        return identifierName;
    }

    public static EngineSelection safeValueOf(String name) {
        try {
            return EngineSelection.valueOf(name);
        } catch (IllegalArgumentException _) {
            return EngineSelection.OCRMYPDF;
        }
    }
}
