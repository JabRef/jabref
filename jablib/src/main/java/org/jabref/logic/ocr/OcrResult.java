package org.jabref.logic.ocr;

import java.nio.file.Path;
import java.util.List;

/// Represents the result of an OCR operation.
///
/// Uses sealed classes to ensure type safety and avoid null parameters.
public sealed interface OcrResult {
    /// Represents a successful OCR result.
    ///
    /// Contains the path of the searchable PDF created by the OCR engine.
    record Success(Path outputFile) implements OcrResult {
    }

    /// Represents a failed OCR result.
    ///
    /// Contains the failure reason and diagnostic information from the OCR process.
    record Failure(OcrFailureReason reason, List<String> command, String output) implements OcrResult {
    }

    /// Checks if this result is success.
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /// Checks if this result is failure.
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /// Factory method to create a success result with text and output file.
    static OcrResult success(Path outputFile) {
        return new Success(outputFile);
    }

    /// Factory method to create a failure result without process diagnostics.
    static OcrResult failure(OcrFailureReason reason) {
        return new Failure(reason, List.of(), "");
    }

    /// Factory method to create a failure result with process diagnostics.
    static OcrResult failure(OcrFailureReason reason, List<String> command, String output) {
        return new Failure(reason, command, output);
    }
}
