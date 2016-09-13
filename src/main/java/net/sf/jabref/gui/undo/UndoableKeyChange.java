package net.sf.jabref.gui.undo;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

/**
 * This class represents a change in any field value. The relevant
 * information is the BibEntry, the field name, the old and the
 * new value. Old/new values can be null.
 */
public class UndoableKeyChange extends AbstractUndoableJabRefEdit {

    private final BibEntry entry;
    private final BibDatabase base;
    private final String oldValue;
    private final String newValue;


    public UndoableKeyChange(BibDatabase base, BibEntry entry,
            String oldValue, String newValue) {
        this.base = base;
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

        // Revert the change.
        set(oldValue);
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        set(newValue);
    }

    private void set(String to) {
        base.setCiteKeyForEntry(entry, to);
    }

}
