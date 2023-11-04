package org.jabref.gui.collab.stringdelete;

import org.jabref.gui.collab.GitChange;
import org.jabref.gui.collab.GitChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableRemoveString;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.GitContext;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BibTexStringDelete extends GitChange {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibTexStringDelete.class);

    private final BibtexString deletedString;

    public BibTexStringDelete(BibtexString deletedString, GitContext databaseContext, GitChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.deletedString = deletedString;
        setChangeName(Localization.lang("Deleted string: '%0'", deletedString.getName()));
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        try {
            databaseContext.getGit().removeString(deletedString.getId());
            undoEdit.addEdit(new UndoableRemoveString(databaseContext.getGit(), deletedString));
        } catch (Exception ex) {
            LOGGER.warn("Error: could not remove string '{}': {}", deletedString.getName(), ex.getMessage(), ex);
        }
    }

    public BibtexString getDeletedString() {
        return deletedString;
    }
}
