package org.jabref.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefException extends Exception {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefException.class);
    private String localizedMessage;

    public JabRefException(String message) {
        super(message);
    }

    public JabRefException(String message, Throwable cause) {
        super(message, cause);
    }

    public JabRefException(String message, String localizedMessage) {
        super(message);
        this.localizedMessage = localizedMessage;
    }

    public JabRefException(String message, String localizedMessage, Throwable cause) {
        super(message, cause);
        this.localizedMessage = localizedMessage;
    }

    public JabRefException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getLocalizedMessage() {
        if (localizedMessage == null) {
            LOGGER.debug("No localized exception message defined. Falling back to getMessage().");
            return getMessage();
        } else {
            return localizedMessage;
        }
    }
}
