package org.jabref;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JabRefException extends Exception {

    private static final Log LOGGER = LogFactory.getLog(JabRefException.class);
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
            LOGGER.debug("No localized message exception message defined. Falling back to getMessage().");
            return getMessage();
        } else {
            return localizedMessage;
        }
    }

}
