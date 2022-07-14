package org.jabref.logic.importer;

/**
 *  Should be thrown when you encounter a http status code error >= 400 and < 500
 */
public class FetcherClientException extends FetcherException {

    public FetcherClientException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

    public FetcherClientException(String errorMessage) {
        super(errorMessage);
    }

    public FetcherClientException(String errorMessage, String localizedMessage, Throwable cause) {
        super(errorMessage, localizedMessage, cause);
    }

}
