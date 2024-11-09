package org.jabref.logic.git;

import org.jabref.logic.JabRefException;

public class GitException extends JabRefException {
    public GitException(String message) {
        super(message);
    }

    public GitException(String message, Throwable cause) {
        super(message, cause);
    }

    public GitException(String message, String localizedMessage) {
        super(message, localizedMessage);
    }

    public GitException(String message, String localizedMessage, Throwable cause) {
        super(message, localizedMessage, cause);
    }

    public GitException(Throwable cause) {
        super(cause);
    }
}
