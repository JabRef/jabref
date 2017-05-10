package org.jabref.gui.undo;

import javax.swing.undo.AbstractUndoableEdit;

import org.jabref.logic.l10n.Localization;

public class AbstractUndoableJabRefEdit extends AbstractUndoableEdit {

    @Override
    public String getUndoPresentationName() {
        return "<html>" + Localization.lang("Undo") + ": " + getPresentationName() + "</html>";
    }

    @Override
    public String getRedoPresentationName() {
        return "<html>" + Localization.lang("Redo") + ": " + getPresentationName() + "</html>";
    }

}
