package org.jabref.logic.ocr.exception;

import org.jabref.logic.JabRefException;

/**
 * Exception thrown when OCR processing fails.
 * <p>
 * Follows JabRef's exception hierarchy by extending JabRefException.
 */
public class OcrProcessException extends JabRefException {
    
    /**
     * Create a new OCR process exception.
     *
     * @param message The error message
     */
    public OcrProcessException(String message) {
        super(message);
    }
    
    /**
     * Create a new OCR process exception with a cause.
     *
     * @param message The error message
     * @param cause The underlying cause
     */
    public OcrProcessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Create a new OCR process exception for a specific engine.
     *
     * @param engineName Name of the OCR engine
     * @param message The error message
     */
    public OcrProcessException(String engineName, String message) {
        super(String.format("OCR engine '%s': %s", engineName, message));
    }
    
    /**
     * Create a new OCR process exception for a specific engine with a cause.
     *
     * @param engineName Name of the OCR engine
     * @param message The error message
     * @param cause The underlying cause
     */
    public OcrProcessException(String engineName, String message, Throwable cause) {
        super(String.format("OCR engine '%s': %s", engineName, message), cause);
    }
}