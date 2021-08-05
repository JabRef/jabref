package org.jabref.gui.openoffice;

/**
 * Exception used to indicate that the plugin attempted to set a character format that is
 * not defined in the current OpenOffice document.
 */
class CreationException extends Exception {

    public CreationException(String message) {
        super(message);
    }

}
