package org.jabref.logic.ocr;

import org.jabref.logic.ocr.docling.DoclingEngine;

/// Factory for any engine that implements {@code OcrEngine}
public final class OcrEngineFactory {

    public static OcrEngine create(OcrPreferences preferences, EngineSelection selection) {
        return switch (selection) {
            case DOCLING ->
                    new DoclingEngine(preferences);
            case OCRMYPDF ->
                    new OcrMyPdfEngine(preferences);
        };
    }
}
