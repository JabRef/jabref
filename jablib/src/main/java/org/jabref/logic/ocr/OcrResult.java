package org.jabref.logic.ocr;

import java.nio.file.Path;

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
    /// Contains the reason why the failure occurred that the GUI part can localize it and output to the user.
    record Failure(OcrFailureReason reason) implements OcrResult {
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

    /// Factory method to create a failure result with an error message.
    static OcrResult failure(OcrFailureReason reason) {
        return new Failure(reason);
    }
}
