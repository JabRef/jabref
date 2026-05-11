package org.jabref.logic.ocr;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents the result of an OCR operation.
 * Uses sealed classes to ensure type safety and avoid null parameters.
 */
public sealed interface OcrResult {
    /**
     * Represents a successful OCR result, containing the recognized text and optional output file.
     */
    record Success(String text, Path outputFile) implements OcrResult {
        public Success(String text) {
            this(text, null);
        }
    }

    /**
     * Represents a failed OCR result with an error message.
     */
    record Failure(String errorMessage) implements OcrResult {
        public Failure {
            if (StringUtils.isBlank(errorMessage)) {
                errorMessage = "Unknown error during OCR process";
            }
        }
    }

    /**
     * Checks if this result is success.
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Checks if this result is failure.
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Factory method to create a success result with text only.
     */
    static OcrResult success(String text) {
        return new Success(text);
    }

    /**
     * Factory method to create a success result with text and output file.
     */
    static OcrResult success(String text, Path outputFile) {
        return new Success(text, outputFile);
    }

    /**
     * Factory method to create a failure result with an error message.
     */
    static OcrResult failure(String errorMessage) {
        return new Failure(errorMessage);
    }
}
