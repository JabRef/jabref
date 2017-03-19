package org.jabref.gui.openoffice;

/**
 * This exception is used to indicate that connection to OpenOffice has been lost.
 */
class ConnectionLostException extends RuntimeException {

    public ConnectionLostException(String s) {
        super(s);
    }
}
