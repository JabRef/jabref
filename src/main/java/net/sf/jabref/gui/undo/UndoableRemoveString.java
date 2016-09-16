package net.sf.jabref.gui.undo;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.KeyCollisionException;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UndoableRemoveString extends AbstractUndoableJabRefEdit {

    private final BibDatabase base;
    private final BibtexString string;
    private final BasePanel panel;

    private static final Log LOGGER = LogFactory.getLog(UndoableRemoveString.class);

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
            LOGGER.warn("Problem to undo `remove string`", ex);
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
