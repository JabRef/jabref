package org.jabref.gui.collab.stringdelete;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableRemoveString;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BibTexStringDelete extends DatabaseChange {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibTexStringDelete.class);

    private final BibtexString deletedString;

    public BibTexStringDelete(BibtexString deletedString, BibDatabaseContext databaseContext, DatabaseChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.deletedString = deletedString;
        setChangeName(Localization.lang("Deleted string: '%0'", deletedString.getName()));
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        try {
            databaseContext.getDatabase().removeString(deletedString.getId());
            undoEdit.addEdit(new UndoableRemoveString(databaseContext.getDatabase(), deletedString));
        } catch (Exception ex) {
            LOGGER.warn("Error: could not remove string '{}': {}", deletedString.getName(), ex.getMessage(), ex);
        }
    }

    public BibtexString getDeletedString() {
        return deletedString;
    }
}
