package org.jabref.logic.importer;

public class DOIDataNotFoundException extends FetcherException {

    public DOIDataNotFoundException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

    public DOIDataNotFoundException(String errorMessage) {
        super(errorMessage);
    }

    public DOIDataNotFoundException(String errorMessage, String localizedMessage, Throwable cause) {
        super(errorMessage, localizedMessage, cause);
    }
}
