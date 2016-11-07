package net.sf.jabref.gui.undo;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.strings.StringUtil;

public class UndoableStringChange extends AbstractUndoableJabRefEdit {

    private final BibtexString string;
    private final String oldValue;
    private final String newValue;
    private final boolean nameChange;
    private final BasePanel panel;


    public UndoableStringChange(BasePanel panel,
            BibtexString string, boolean nameChange,
            String oldValue, String newValue) {
        this.string = string;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.nameChange = nameChange;
        this.panel = panel;
    }

    @Override
    public String getPresentationName() {
        return (nameChange ? Localization.lang("change string name %0 to %1", StringUtil.boldHTML(oldValue),
                StringUtil.boldHTML(newValue)) : Localization.lang("change string content %0 to %1",
                        StringUtil.boldHTML(oldValue), StringUtil.boldHTML(newValue)));
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.

        panel.assureStringDialogNotEditing();

        if (nameChange) {
            string.setName(oldValue);
        } else {
            string.setContent(oldValue);
        }

        panel.updateStringDialog();
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.

        panel.assureStringDialogNotEditing();
        if (nameChange) {
            string.setName(newValue);
        } else {
            string.setContent(newValue);
        }

        panel.updateStringDialog();
    }

}
