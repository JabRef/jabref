package net.sf.jabref;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JabRefException extends Exception {

    private String localizedMessage;

    private static final Log LOGGER = LogFactory.getLog(JabRefException.class);

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

    @Override
    public String getLocalizedMessage() {
        if (localizedMessage == null) {
            LOGGER.warn("No localized message exception message defined. Falling back to getMessage().");
            return getMessage();
        } else {
            return localizedMessage;
        }
    }

}
