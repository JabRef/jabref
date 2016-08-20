package net.sf.jabref.logic.importer;

public class FetcherException extends Exception {


    public FetcherException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
    }

    public FetcherException(String errorMessage) {
        super(errorMessage);
    }
}
