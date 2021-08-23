package org.jabref.model.openoffice.uno;

import java.util.Optional;

import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerSupplier;
import com.sun.star.text.XTextDocument;
import com.sun.star.util.InvalidStateException;

/**
 * Undo : group document changes into larger Undo actions.
 */
public class UnoUndo {

    private UnoUndo() { }

    public static Optional<XUndoManager> getXUndoManager(XTextDocument doc) {
        // https://www.openoffice.org/api/docs/common/ref/com/sun/star/document/XUndoManager.html
        return (UnoCast.cast(XUndoManagerSupplier.class, doc)
                .map(XUndoManagerSupplier::getUndoManager));
    }

    /**
     * Each call to enterUndoContext must be paired by a call to leaveUndoContext, otherwise, the
     * document's undo stack is left in an inconsistent state.
     */
    public static void enterUndoContext(XTextDocument doc, String title) {
        Optional<XUndoManager> undoManager = getXUndoManager(doc);
        if (undoManager.isPresent()) {
            undoManager.get().enterUndoContext(title);
        }
    }

    public static void leaveUndoContext(XTextDocument doc) {
        Optional<XUndoManager> undoManager = getXUndoManager(doc);
        if (undoManager.isPresent()) {
            try {
                undoManager.get().leaveUndoContext();
            } catch (InvalidStateException ex) {
                throw new IllegalStateException("leaveUndoContext reported InvalidStateException");
            }
        }
    }
}
