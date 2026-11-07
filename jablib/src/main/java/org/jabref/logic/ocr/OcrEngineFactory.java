package org.jabref.logic.ocr;

import org.jabref.logic.ocr.docling.DoclingEngine;

import org.jspecify.annotations.NullMarked;

/// Factory for any engine that implements [OcrEngine]
@NullMarked
public final class OcrEngineFactory {

    @SuppressWarnings("checkstyle:NoWhitespaceBefore")
    public static OcrEngine create(OcrPreferences preferences) {
        return switch (preferences.getEngineSelection()) {
            case DOCLING ->
                    new DoclingEngine(preferences);
            case TESSERACT,
                 EASYOCR,
                 PADDLEOCR,
                 APPLEOCR ->
                    new OcrMyPdfEngine(preferences);
        };
    }
}
