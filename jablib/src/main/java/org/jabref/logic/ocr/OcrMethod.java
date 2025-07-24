package org.jabref.logic.ocr;

/**
 * Available methods for creating searchable PDFs.
 */
public enum OcrMethod {
    PDFBOX("PDFBox (Built-in)", "Uses built-in PDFBox library. Fast but text positioning may be imperfect."),
    OCRMYPDF("ocrmypdf (External)", "Uses external ocrmypdf tool. Produces high-quality searchable PDFs with accurate text positioning.");

    private final String displayName;
    private final String description;

    OcrMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
