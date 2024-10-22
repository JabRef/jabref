package org.jabref.logic.bibtex;

/**
 * Use only if you know what you are doing.
 *
 * Otherwise, you should implement your functionality as {@link org.jabref.logic.integrity.IntegrityCheck} instead.
 */
public class InvalidFieldValueException extends Exception {

    public InvalidFieldValueException(String message) {
        super(message);
    }
}
