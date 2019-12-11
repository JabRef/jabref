package org.jabref.logic.importer;

import org.jabref.JabRefException;

public class NoFetcherFoundException extends RuntimeException {

    public NoFetcherFoundException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

    public NoFetcherFoundException(String errorMessage) {
        super(errorMessage);
    }

}
