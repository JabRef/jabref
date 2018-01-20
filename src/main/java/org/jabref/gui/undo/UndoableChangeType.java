package org.jabref.gui.undo;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

/**
 * This class represents the change of type for an entry.
 */
public class UndoableChangeType extends AbstractUndoableJabRefEdit {
    private final String oldType;
    private final String newType;
    private final BibEntry entry;

    public UndoableChangeType(FieldChange change) {
        this(change.getEntry(), change.getOldValue(), change.getNewValue());
    }

    public UndoableChangeType(BibEntry entry, String oldType, String newType) {
        this.oldType = oldType;
        this.newType = newType;
        this.entry = entry;
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("change type of entry %0 from %1 to %2",
                StringUtil.boldHTML(entry.getCiteKeyOptional().orElse(Localization.lang("undefined"))),
                StringUtil.boldHTML(oldType, Localization.lang("undefined")),
                StringUtil.boldHTML(newType));
    }

    @Override
    public void undo() {
        super.undo();
        entry.setType(oldType);
    }

    @Override
    public void redo() {
        super.redo();
        entry.setType(newType);
    }
}
