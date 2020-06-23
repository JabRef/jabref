package org.jabref.model.util;

import org.jabref.JabRefException;

public class WatchServiceUnavailableException extends JabRefException {
    public WatchServiceUnavailableException(final String message, final String localizedMessage, final Throwable cause) {
        super(message, localizedMessage, cause);
    }
}
