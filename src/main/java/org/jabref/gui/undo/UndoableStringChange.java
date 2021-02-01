package org.jabref.gui.undo;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.strings.StringUtil;

public class UndoableStringChange extends AbstractUndoableJabRefEdit {

    private final BibtexString string;
    private final String oldValue;
    private final String newValue;
    private final boolean nameChange;

    public UndoableStringChange(BibtexString string, boolean nameChange, String oldValue, String newValue) {
        this.string = string;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.nameChange = nameChange;
    }

    @Override
    public String getPresentationName() {
        return (nameChange ? Localization.lang("change string name %0 to %1", StringUtil.boldHTML(oldValue),
                StringUtil.boldHTML(newValue)) :
                Localization.lang("change string content %0 to %1",
                        StringUtil.boldHTML(oldValue), StringUtil.boldHTML(newValue)));
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        if (nameChange) {
            string.setName(oldValue);
        } else {
            string.setContent(oldValue);
        }
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        if (nameChange) {
            string.setName(newValue);
        } else {
            string.setContent(newValue);
        }
    }
}
