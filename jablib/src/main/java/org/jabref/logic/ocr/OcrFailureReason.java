package org.jabref.logic.ocr;

/// Enums for the reasons that can lead the OCR process to fail.
public enum OcrFailureReason {
    NOT_AVAILABLE(1), TIMEOUT(2), NON_ZERO_EXIT(3), IO_ERROR(4), INTERRUPTED(5);

    private final int errorCode;

    OcrFailureReason(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
