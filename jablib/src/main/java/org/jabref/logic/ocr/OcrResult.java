package org.jabref.logic.ocr;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.strings.StringUtil;
import org.jspecify.annotations.NonNull;

/**
 * Represents the result of an OCR operation.
 * Uses sealed classes to ensure type safety and avoid null parameters.
 */
public sealed interface OcrResult {
    /**
     * Represents a successful OCR operation with extracted text and optional output file.
     */
    record Success(@NonNull String text, Optional<Path> outputFile) implements OcrResult {
        public Success(@NonNull String text) {
            this(text, Optional.empty());
        }
    }

    /**
     * Represents a failed OCR operation with an error message.
     */
    record Failure(String errorMessage) implements OcrResult {
        public Failure {
            // Provide default message instead of throwing exception
            if (StringUtil.isBlank(errorMessage)) {
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
     * Factory method for creating a success result with text only.
     */
    static OcrResult success(@NonNull String text) {
        return new Success(text);
    }

    /**
     * Factory method for creating a success result with text and output file.
     */
    static OcrResult success(@NonNull String text, Path outputFile) {
        return new Success(text, Optional.of(outputFile));
    }

    /**
     * Factory method for creating a failure result.
     */
    static OcrResult failure(String errorMessage) {
        return new Failure(errorMessage);
    }
}
