package org.jabref.logic.ocr;

/// Enums for the reasons that can lead the OCR process to fail.
public enum OcrFailureReason {
    NOT_AVAILABLE, TIMEOUT, NON_ZERO_EXIT, IO_ERROR, INTERRUPTED
}
