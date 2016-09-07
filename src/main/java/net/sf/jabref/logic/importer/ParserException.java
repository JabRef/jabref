package net.sf.jabref.logic.importer;

public class ParserException extends Exception {


    public ParserException(String errorMessage, Exception cause) {
        super(errorMessage, cause);
    }

    public ParserException(String errorMessage) {
        super(errorMessage);
    }

    public ParserException(Exception cause) {
        super(cause);
    }
}
