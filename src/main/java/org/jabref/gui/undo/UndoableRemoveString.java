package org.jabref.gui.undo;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndoableRemoveString extends AbstractUndoableJabRefEdit {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoableRemoveString.class);
    private final BibDatabase base;
    private final BibtexString string;

    public UndoableRemoveString(BibDatabase base, BibtexString string) {
        this.base = base;
        this.string = string;
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
    }

    @Override
    public void redo() {
        super.redo();
        // Redo the change.
        base.removeString(string.getId());
    }
}
