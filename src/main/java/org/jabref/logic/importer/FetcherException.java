package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.http.dto.SimpleHttpResponse;
import org.jabref.logic.JabRefException;

public class FetcherException extends JabRefException {

    private final SimpleHttpResponse httpResponse;

    public FetcherException(SimpleHttpResponse httpResponse) {
        super(httpResponse.responseMessage());
        this.httpResponse = httpResponse;
    }

    public FetcherException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        httpResponse = null;
    }

    public FetcherException(String errorMessage) {
        super(errorMessage);
        httpResponse = null;
    }

    public FetcherException(String errorMessage, String localizedMessage, Throwable cause) {
        super(errorMessage, localizedMessage, cause);
        httpResponse = null;
    }

    @Override
    public String getLocalizedMessage() {
        // TODO: This should be moved to a separate class converting "any" exception object to a localized message
        if (httpResponse != null) {
            return "Encountered HTTP %s %s (%s)".formatted(httpResponse.statusCode(), httpResponse.responseMessage(), httpResponse.responseBody());
        } else {
            return super.getLocalizedMessage();
        }
    }

    public Optional<SimpleHttpResponse> getHttpResponse() {
        return Optional.ofNullable(httpResponse);
    }
}
