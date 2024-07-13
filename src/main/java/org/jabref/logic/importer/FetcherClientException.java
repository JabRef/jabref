package org.jabref.logic.importer;

import org.jabref.http.dto.SimpleHttpResponse;

/**
 * Should be thrown when you encounter an HTTP status code error &gt;= 400 and &lt; 500.
 */
public class FetcherClientException extends FetcherException {
    private SimpleHttpResponse httpResponse;

    public FetcherClientException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
    }

    public FetcherClientException(String errorMessage, Throwable cause, SimpleHttpResponse httpResponse) {
        super(errorMessage, cause);
        this.httpResponse = httpResponse;
    }

    public FetcherClientException(String errorMessage) {
        super(errorMessage);
    }

    public FetcherClientException(String errorMessage, String localizedMessage, Throwable cause) {
        super(errorMessage, localizedMessage, cause);
    }

    public FetcherClientException(String errorMessage,SimpleHttpResponse httpResponse) {
        super(errorMessage);
        this.httpResponse=httpResponse;
    }

    public SimpleHttpResponse getHttpResponse() {
        return httpResponse;
    }

}
