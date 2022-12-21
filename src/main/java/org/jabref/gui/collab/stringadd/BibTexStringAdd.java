package org.jabref.gui.collab.stringadd;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.KeyCollisionException;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BibTexStringAdd extends DatabaseChange {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibTexStringAdd.class);

    private final BibtexString addedString;

    public BibTexStringAdd(BibtexString addedString, BibDatabaseContext databaseContext, DatabaseChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.addedString = addedString;
        setChangeName(Localization.lang("Added string: '%0'", addedString.getName()));
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        try {
            databaseContext.getDatabase().addString(addedString);
            undoEdit.addEdit(new UndoableInsertString(databaseContext.getDatabase(), addedString));
        } catch (KeyCollisionException ex) {
            LOGGER.warn("Error: could not add string '{}': {}", addedString.getName(), ex.getMessage(), ex);
        }
    }

    public BibtexString getAddedString() {
        return addedString;
    }
}
