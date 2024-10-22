package org.jabref.logic.bibtex;

/**
 * @deprecated Use only if you know what you are doing.
 *             Otherwise, you should implement your functionality as {@link org.jabref.logic.integrity.IntegrityCheck} instead.
 *             The JabRef team leaves the <code>@deprecated</code> annotation to have IntelliJ listing this method with a strike-through.
 */
@Deprecated
public class InvalidFieldValueException extends Exception {

    public InvalidFieldValueException(String message) {
        super(message);
    }
}
