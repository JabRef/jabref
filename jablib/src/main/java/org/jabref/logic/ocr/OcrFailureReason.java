package org.jabref.logic.ocr;

import org.jabref.logic.l10n.Localization;

/// Enums for the reasons that can lead the OCR process to fail.
public enum OcrFailureReason {
    NOT_AVAILABLE, TIMEOUT, NON_ZERO_EXIT, IO_ERROR, INTERRUPTED;

    public String getMessage(String engineName, String enginePath) {
        return switch (this) {
            case NOT_AVAILABLE ->
                    Localization.lang("%0 is not available at: %1", engineName, enginePath);
            case TIMEOUT ->
                    Localization.lang("OCR timed out");
            case NON_ZERO_EXIT ->
                    Localization.lang("OCR process failed");
            case IO_ERROR ->
                    Localization.lang("Could not start OCR process");
            case INTERRUPTED ->
                    Localization.lang("OCR was cancelled");
        };
    }
}
