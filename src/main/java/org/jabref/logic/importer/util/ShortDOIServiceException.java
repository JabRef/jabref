package org.jabref.logic.importer.util;

import org.jabref.logic.JabRefException;

public class ShortDOIServiceException extends JabRefException {
    public ShortDOIServiceException(String message) {
        super(message);
    }

    public ShortDOIServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShortDOIServiceException(String message, String localizedMessage) {
        super(message, localizedMessage);
    }

    public ShortDOIServiceException(String message, String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public ShortDOIServiceException(Throwable cause) {
        super(cause);
    }
}
