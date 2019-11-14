package org.jabref.logic.net;

import org.jabref.JabRefException;

public class GrobidClientException extends JabRefException {

    public GrobidClientException() {
        super("An error occurred while processing your query.");
    }

    public GrobidClientException(String message) {
        super(message);
    }

    public static GrobidClientException getNewGrobidClientExceptionByCode(int httpCode) {
        switch(httpCode) {
            case 204:
                return new GrobidClientException("The GROBID service could not extract any Information from this String.");
            case 400:
                return new GrobidClientException("The generated Request was wrong.");
            case 500:
                return new GrobidClientException("An internal GROBID service error occured.");
            case 503:
                return new GrobidClientException("There are too many requests at a time, please try again later.");
            default:
                return new GrobidClientException("An error occured processing your GROBID request.");
        }
    }

}
