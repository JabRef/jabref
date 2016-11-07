package net.sf.jabref.gui.undo;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.strings.StringUtil;

/**
 * This class represents a change in any field value. The relevant
 * information is the BibEntry, the field name, the old and the
 * new value. Old/new values can be null.
 */
public class UndoableKeyChange extends AbstractUndoableJabRefEdit {

    private final BibEntry entry;
    private final String oldValue;
    private final String newValue;


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
