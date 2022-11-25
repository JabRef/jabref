package org.jabref.gui.undo;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;

/**
 * This class represents a change in any field value. The relevant information is the BibEntry, the field name, the old and the new value. Old/new values can be null.
 */
public class UndoablePreambleChange extends AbstractUndoableJabRefEdit {

    private final BibDatabase base;
    private final String oldValue;
    private final String newValue;

    public UndoablePreambleChange(BibDatabase base,
                                  String oldValue, String newValue) {
        this.base = base;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("change preamble");
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        base.setPreamble(oldValue);
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        base.setPreamble(newValue);
    }
}
