package net.sf.jabref.logic.journals;

import net.sf.jabref.JabRefException;


/**
 * This exception will be thrown if the same journal abbreviation already exists in the same list.
 */
public class DuplicatedJournalAbbreviationException extends JabRefException {

    public DuplicatedJournalAbbreviationException(String message) {
        super(message);
    }

}
