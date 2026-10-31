package org.jabref.logic;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class JabRefException extends Exception {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefException.class);
    private @Nullable String localizedMessage;

    public JabRefException(String message) {
        super(message);
    }

    // A `null` value for `cause` is permitted, as per [Exception] documentation.
    public JabRefException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public JabRefException(String message, @Nullable String localizedMessage) {
        super(message);
        this.localizedMessage = localizedMessage;
    }

    public JabRefException(String message, @Nullable String localizedMessage, @Nullable Throwable cause) {
        super(message, cause);
        this.localizedMessage = localizedMessage;
    }

    public JabRefException(Throwable cause) {
        super(cause);
    }

    // Returning `null` because [Exception]/[Throwable] can also return `null`.
    @Override
    public @Nullable String getLocalizedMessage() {
        if (localizedMessage == null) {
            String message = getMessage();
            LOGGER.debug("No localized exception message defined. Falling back to getMessage() ({}).", message);
            return message;
        } else {
            return localizedMessage;
        }
    }
}
