package org.jabref.toolkit.exception;

import org.jspecify.annotations.NullMarked;

/// An ExportServiceException signals an error when exporting to a different format or the underlying filesystem or terminal.
@NullMarked
public class ExportServiceException extends CliException {
    public ExportServiceException(String message, String localizedMessage, int exitCode) {
        super(message, localizedMessage, exitCode);
    }

    public ExportServiceException(String message, String localizedMessage, Throwable cause, int exitCode) {
        super(message, localizedMessage, cause, exitCode);
    }
}
