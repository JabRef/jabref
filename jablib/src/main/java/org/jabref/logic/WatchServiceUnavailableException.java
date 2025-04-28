package org.jabref.logic;

public class WatchServiceUnavailableException extends JabRefException {
    public WatchServiceUnavailableException(final String message, final String localizedMessage, final Throwable cause) {
        super(message, localizedMessage, cause);
    }
}
