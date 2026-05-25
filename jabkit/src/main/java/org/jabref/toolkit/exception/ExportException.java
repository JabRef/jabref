package org.jabref.toolkit.exception;

/// An ExportException signals an error when exporting to a different format or the underlying filesystem or terminal.
public class ExportException extends CliException {
    public ExportException(String message, String localizedMessage, int exitCode) {
        super(message, localizedMessage, exitCode);
    }

    public ExportException(String message, String localizedMessage, Throwable cause, int exitCode) {
        super(message, localizedMessage, cause, exitCode);
    }
}
