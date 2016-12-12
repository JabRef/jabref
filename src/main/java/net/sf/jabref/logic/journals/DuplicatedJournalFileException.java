package net.sf.jabref.logic.journals;

import net.sf.jabref.JabRefException;

/**
 * This exception will be thrown if the same journal abbreviation file is already opened.
 */
public class DuplicatedJournalFileException extends JabRefException {

    public DuplicatedJournalFileException(String message) {
        super(message);
    }

}
