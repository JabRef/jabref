package org.jabref.logic.importer.util;

import org.jabref.JabRefException;

public class GrobidServiceException extends JabRefException {

    public GrobidServiceException(String message) {
        super(message);
    }

    public GrobidServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public static GrobidServiceException getNewGrobidClientExceptionByCode(int httpCode) {
        switch(httpCode) {
            case 204:
                return new GrobidServiceException("The GROBID service could not extract any Information from this String.");
            case 400:
                return new GrobidServiceException("The generated Request was wrong.");
            case 500:
                return new GrobidServiceException("An internal GROBID service error occured.");
            case 503:
                return new GrobidServiceException("There are too many requests at a time, please try again later.");
            default:
                return new GrobidServiceException("An error occured processing your GROBID request.");
        }
    }

}
