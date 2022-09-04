package org.jabref.gui.collab.stringchange;

import org.jabref.gui.collab.ExternalChange;
import org.jabref.gui.collab.ExternalChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableStringChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BibTexStringChange extends ExternalChange {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibTexStringChange.class);

    private final BibtexString oldString;
    private final BibtexString newString;

    public BibTexStringChange(BibtexString oldString, BibtexString newString, BibDatabaseContext databaseContext, ExternalChangeResolverFactory externalChangeResolverFactory) {
        super(databaseContext, externalChangeResolverFactory);
        this.oldString = oldString;
        this.newString = newString;

        setChangeName(Localization.lang("Modified string: '%0'", oldString.getName()));
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        String oldContent = oldString.getContent();
        String newContent = newString.getContent();
        oldString.setContent(newContent);
        undoEdit.addEdit(new UndoableStringChange(oldString, false, oldContent, newContent));
    }

    public BibtexString getOldString() {
        return oldString;
    }

    public BibtexString getNewString() {
        return newString;
    }
}
