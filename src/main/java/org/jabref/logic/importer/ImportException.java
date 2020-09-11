package org.jabref.logic.importer;

import org.jabref.logic.JabRefException;

public class ImportException extends JabRefException {

    public ImportException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
    }

    public ImportException(String errorMessage) {
        super(errorMessage);
    }

    public ImportException(Exception cause) {
        super(cause);
    }
}
