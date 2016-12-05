package net.sf.jabref.logic.journals;

import net.sf.jabref.JabRefException;


/**
 * This exception will be thrown if any of the mandatory fields for an {@link Abbreviation}
 * object are empty. The mandatory fields are {@code name} and {@code abbreviation}.
 *
 */
public class EmptyFieldException extends JabRefException {

    public EmptyFieldException(String message) {
        super(message);
    }

}
