package org.jabref.logic.ocr;

import java.util.Optional;

public class OcrResult {
    private final boolean success;
    private final String text;
    private final String errorMessage;

    private OcrResult(boolean success, String text, String errorMessage) {
        this.success = success;
        this.text = text;
        this.errorMessage = errorMessage;
    }

    public static OcrResult success(String text) {
        return new OcrResult(true, text, null);
    }

    public static OcrResult failure(String errorMessage) {
        return new OcrResult(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public Optional<String> getText() {
        return Optional.ofNullable(text);
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
