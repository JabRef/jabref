package org.jabref.logic.ocr;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum OcrMyPdfPlugin {
    EASYOCR("ocrmypdf_easyocr", "EasyOCR"),
    TESSERACT("", "Tesseract (default)");

    private final String identifierName;
    private final String displayName;

    OcrMyPdfPlugin(String identifierName, String displayName) {
        this.identifierName = identifierName;
        this.displayName = displayName;
    }

    public String getIdentifierName() {
        return identifierName;
    }

    public static OcrMyPdfPlugin safeValueOf(String name) {
        try {
            return OcrMyPdfPlugin.valueOf(name);
        } catch (IllegalArgumentException _) {
            return OcrMyPdfPlugin.TESSERACT;
        }
    }

    public String getDisplayName() {
        return displayName;
    }
}
