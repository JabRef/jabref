package org.jabref.logic.importer;

public class DOIServerNotAvailableException extends FetcherException {
    public DOIServerNotAvailableException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

    public DOIServerNotAvailableException(String errorMessage) {
        super(errorMessage);
    }

    public DOIServerNotAvailableException(String errorMessage, String localizedMessage, Throwable cause) {
        super(errorMessage, localizedMessage, cause);
    }
}
