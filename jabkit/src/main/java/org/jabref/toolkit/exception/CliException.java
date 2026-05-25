package org.jabref.toolkit.exception;

import org.jabref.logic.JabRefException;

/// CLI-Exceptions that will be handled by PicoCLI and presented to the user (a localizable message is mandatory).
public class CliException extends JabRefException {
    private final int exitCode;

    /// Creates a new CliException with a given localized message to present to the user.
    public CliException(String message, String localizedMessage, int exitCode) {
        super(message, localizedMessage);
        this.exitCode = exitCode;
    }

    /// Creates a new CliException with a given localized message to present to the user and the cause of the Exception.
    public CliException(String message, String localizedMessage, Throwable cause, int exitCode) {
        super(message, localizedMessage, cause);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}
