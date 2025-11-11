package org.jabref.gui.undo;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.strings.StringUtil;

/**
 * This class represents the change of type for an entry.
 */
public class UndoableChangeType extends AbstractUndoableJabRefEdit {
    private final EntryType oldType;
    private final EntryType newType;
    private final BibEntry entry;

    public UndoableChangeType(FieldChange change) {
        this(change.getEntry(), EntryTypeFactory.parse(change.getOldValue()), EntryTypeFactory.parse(change.getNewValue()));
    }

    public UndoableChangeType(BibEntry entry, EntryType oldType, EntryType newType) {
        this.oldType = oldType;
        this.newType = newType;
        this.entry = entry;
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("change type of entry %0 from %1 to %2",
                StringUtil.boldHTML(entry.getCitationKey().orElse(Localization.lang("undefined"))),
                StringUtil.boldHTML(oldType.getDisplayName(), Localization.lang("undefined")),
                StringUtil.boldHTML(newType.getDisplayName()));
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
