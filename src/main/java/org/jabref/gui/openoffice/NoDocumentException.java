package org.jabref.gui.openoffice;

/**
 * Exception used to indicate that the plugin attempted to set a character format that is
 * not defined in the current OpenOffice document.
 */
class NoDocumentException extends Exception {

    public NoDocumentException(String message) {
        super(message);
    }

}
