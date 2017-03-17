package org.jabref.logic.bibtex;

/**
 * @deprecated implement as {@link org.jabref.logic.integrity.IntegrityCheck} instead.
 */
@Deprecated
public class InvalidFieldValueException extends Exception {

    public InvalidFieldValueException(String message) {
        super(message);
    }
}
