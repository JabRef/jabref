package net.sf.jabref.oo;

/**
 * This exception is used to indicate that connection to OpenOffice has been lost.
 */
public class ConnectionLostException extends RuntimeException {

    public ConnectionLostException(String s) {
        super(s);
    }
}
