package org.jabref.logic.ocr;

import org.jabref.logic.l10n.Localization;

/// Enums for the reasons that can lead the OCR process to fail.
public enum OcrFailureReason {
    NOT_AVAILABLE("%0 is not available at: %1"),
    TIMEOUT("OCR timed out"),
    NON_ZERO_EXIT("OCR process failed"),
    IO_ERROR("Could not start OCR process"),
    INTERRUPTED("OCR was cancelled");

    private final String message;

    OcrFailureReason(String message) {
        this.message = message;
    }

    public String getMessage(Object... args) {
        return Localization.lang(message, args);
    }
}
