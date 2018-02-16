package org.jabref.gui.undo;

import org.jabref.gui.BasePanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndoableInsertString extends AbstractUndoableJabRefEdit {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoableInsertString.class);

    private final BibDatabase base;
    private final BasePanel panel;
    private final BibtexString string;


    public UndoableInsertString(BasePanel panel, BibDatabase base,
            BibtexString string) {
        this.base = base;
        this.panel = panel;
        this.string = string;
    }

    @Override
    public String getPresentationName() {
        return Localization.lang("insert string %0", StringUtil.boldHTML(string.toString()));
    }

    @Override
    public void undo() {
        super.undo();

        // Revert the change.
        base.removeString(string.getId());
        panel.updateStringDialog();
    }

    @Override
    public void redo() {
        super.redo();

        // Redo the change.
        try {
            base.addString(string);
        } catch (KeyCollisionException ex) {
            LOGGER.warn("Problem to redo `insert entry`", ex);
        }

        panel.updateStringDialog();
    }

}
