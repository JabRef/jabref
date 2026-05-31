package org.jabref.toolkit.exception;

/// An ExportServiceException signals an error when exporting to a different format or the underlying filesystem or terminal.
public class ImportServiceException extends CliException {
    public ImportServiceException(String message, String localizedMessage, int exitCode) {
        super(message, localizedMessage, exitCode);
    }

    public ImportServiceException(String message, String localizedMessage, Throwable cause, int exitCode) {
        super(message, localizedMessage, cause, exitCode);
    }
}
