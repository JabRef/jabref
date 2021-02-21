package org.jabref.gui.openoffice;

/**
 * Exception used to indicate that
 *
 * OOBibBase.selectDocument : NoDocumentException("No Writer documents found");
 * OOBibBase.getReferenceMarks : NoDocumentException("getReferenceMarks failed");
 */
class NoDocumentException extends Exception {

    public NoDocumentException(String message) {
        super(message);
    }

}
