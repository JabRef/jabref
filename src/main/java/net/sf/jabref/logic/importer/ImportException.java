package net.sf.jabref.logic.importer;

public class ImportException extends Exception {


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
