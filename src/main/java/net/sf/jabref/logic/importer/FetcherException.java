package net.sf.jabref.logic.importer;

import net.sf.jabref.JabRefException;

public class FetcherException extends JabRefException {


    public FetcherException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

    public FetcherException(String errorMessage) {
        super(errorMessage);
    }

    public FetcherException(String errorMessage, String localizedMessage, Throwable cause) {
        super(errorMessage, localizedMessage, cause);
    }
}
