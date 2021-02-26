package org.jabref.gui.openoffice;

/**
 * Exception used to indicate that we cannot manipulate the current
 * document.
 *
 * OOBibBase.selectDocument : NoDocumentException("No Writer documents found");
 * OOBibBase.getReferenceMarks : NoDocumentException("getReferenceMarks failed");
 *
 * Note: similar to ConnectionLostException, but here connection to
 *       OpenOffice may be intact, e.g. if the document is closed, but
 *       OpenOffice is not.
 *
 *       On the other hand: it is not clear what this distinction buys
 *       us, since we do not connect to multiple documents.
 *
 */
class NoDocumentException extends Exception {

    public NoDocumentException(String message) {
        super(message);
    }

}
