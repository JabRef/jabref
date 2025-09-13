package org.jabref.logic.importer;

import java.net.URL;

import org.jabref.model.http.SimpleHttpResponse;

/**
 * Should be thrown when you encounter a http status code error >= 500
 */
public class FetcherServerException extends FetcherException {
    public FetcherServerException(URL source, SimpleHttpResponse httpResponse) {
        super(source, httpResponse);
    }
}
