package org.jabref.logic.ocr;

/**
 * Exception thrown when OCR operations fail.
 * This exception wraps lower-level OCR engine exceptions to provide
 * a consistent interface for error handling throughout JabRef.
 */
public class OcrException extends Exception {

    /**
     * Constructs an OcrException with a message and underlying cause.
     *
     * @param message Descriptive error message
     * @param cause The underlying exception that caused this error
     */
    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs an OcrException with only a message.
     *
     * @param message Descriptive error message
     */
    public OcrException(String message) {
        super(message);
    }
}
