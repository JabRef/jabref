package org.jabref.logic.importer;

/**
 *  Should be thrown when you encounter a http status code error >= 400 and < 500
 */
public class FetcherClientException extends FetcherException {

    private  int statusCode;

    public FetcherClientException(String errorMessage, Throwable cause, int statusCode) {
        super(errorMessage, cause);
        this.statusCode = statusCode;
    }

    public FetcherClientException(String errorMessage, int statusCode) {
        super(errorMessage);
        this.statusCode = statusCode;
    }

    public FetcherClientException(String errorMessage, String localizedMessage, Throwable cause, int statusCode) {
        super(errorMessage, localizedMessage, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

