package org.jabref.logic.importer;
/**
 *  Should be thrown when you encounter a http status code error >= 500
 */
public class FetcherServerException extends FetcherException {

    public FetcherServerException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

    public FetcherServerException(String errorMessage) {
        super(errorMessage);
    }

    public FetcherServerException(String errorMessage, String localizedMessage, Throwable cause) {
        super(errorMessage, localizedMessage, cause);
    }

}
