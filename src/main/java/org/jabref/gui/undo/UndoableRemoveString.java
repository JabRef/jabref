package org.jabref.gui.undo;

import org.jabref.gui.BasePanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.strings.StringUtil;

import com.jcabi.log.Logger;

public class UndoableRemoveString extends AbstractUndoableJabRefEdit {

    private final BibDatabase base;
    private final BibtexString string;

    private final BasePanel panel;

    public UndoableRemoveString(BasePanel panel,
            BibDatabase base, BibtexString string) {
        this.base = base;
        this.string = string;
        this.panel = panel;
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("remove string %0", StringUtil.boldHTML(string.toString()));
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        try {
            base.addString(string);
        } catch (KeyCollisionException ex) {
            Logger.warn(this, "Problem to undo `remove string`: %[exception]s", ex);
        }

        panel.updateStringDialog();
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        base.removeString(string.getId());

        panel.updateStringDialog();
    }

}
