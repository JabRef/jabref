package org.jabref.gui.undo;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

/**
 * This class represents a change in any field value. The relevant
 * information is the BibEntry, the field name, the old and the
 * new value. Old/new values can be null.
 */
public class UndoableKeyChange extends AbstractUndoableJabRefEdit {

    private final BibEntry entry;
    private final String oldValue;
    private final String newValue;

    public UndoableKeyChange(FieldChange change) {
        this(change.getEntry(), change.getOldValue(), change.getNewValue());
    }

    public UndoableKeyChange(BibEntry entry, String oldValue, String newValue) {
        this.entry = entry;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("change key from %0 to %1",
                StringUtil.boldHTML(oldValue, Localization.lang("undefined")),
                StringUtil.boldHTML(newValue, Localization.lang("undefined")));
    }

    @Override
    public void undo() {
        super.undo();
        entry.setCiteKey(oldValue);
    }

    @Override
    public void redo() {
        super.redo();
        entry.setCiteKey(newValue);
    }

}
