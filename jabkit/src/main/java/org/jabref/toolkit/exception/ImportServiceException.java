package org.jabref.toolkit.exception;

import org.jspecify.annotations.NullMarked;

/// An ImportServiceException signals an error when importing a Database from a file.
@NullMarked
public class ImportServiceException extends CliException {
    public ImportServiceException(String message, String localizedMessage, int exitCode) {
        super(message, localizedMessage, exitCode);
    }

    public ImportServiceException(String message, String localizedMessage, Throwable cause, int exitCode) {
        super(message, localizedMessage, cause, exitCode);
    }
}
