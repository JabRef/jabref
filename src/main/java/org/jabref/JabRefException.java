package org.jabref;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Class for handling exceptions in GUI and printing corresponding exception messages
public class JabRefException extends Exception {
    
    // Creating a logger and using it to write messages to log files
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefException.class);
    private String localizedMessage;

    // Passing the exception message to the base class
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
     
    // To display localized message on error
    @Override
    public String getLocalizedMessage() {
        // If there is no localized message
        if (localizedMessage == null) {
            LOGGER.debug("No localized exception message defined. Falling back to getMessage().");
            return getMessage();
        } else {
            return localizedMessage;
        }
    }

}
