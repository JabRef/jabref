package org.jabref.logic.ocr;

/**
 * Represents the result of an OCR operation.
 * Uses sealed classes to ensure type safety and avoid null parameters.
 */
public sealed interface OcrResult {

    /**
     * Represents a successful OCR operation with extracted text.
     */
    record Success(String text) implements OcrResult {
        public Success {
            // Convert null to empty string instead of throwing exception
            if (text == null) {
                text = "";
            }
        }
    }

    /**
     * Represents a failed OCR operation with an error message.
     */
    record Failure(String errorMessage) implements OcrResult {
        public Failure {
            // Provide default message instead of throwing exception
            if (errorMessage == null || errorMessage.isBlank()) {
                errorMessage = "Unknown error occurred";
            }
        }
    }

    /**
     * Checks if this result represents a success.
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Checks if this result represents a failure.
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Factory method for creating a success result.
     */
    static OcrResult success(String text) {
        return new Success(text);
    }

    /**
     * Factory method for creating a failure result.
     */
    static OcrResult failure(String errorMessage) {
        return new Failure(errorMessage);
    }
}
