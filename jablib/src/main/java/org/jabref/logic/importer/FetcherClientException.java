package org.jabref.logic.importer;

import java.net.URL;

import org.jabref.http.dto.SimpleHttpResponse;

/**
 * Should be thrown when you encounter an HTTP status code error &gt;= 400 and &lt; 500.
 */
public class FetcherClientException extends FetcherException {
    public FetcherClientException(URL url, SimpleHttpResponse httpResponse) {
        super(url, httpResponse);
    }
}
