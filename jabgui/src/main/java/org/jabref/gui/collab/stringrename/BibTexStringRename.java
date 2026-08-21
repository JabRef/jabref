package org.jabref.gui.collab.stringrename;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.undo.ChangeRecorder;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.change.UndoableStringChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BibTexStringRename extends DatabaseChange {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibTexStringRename.class);

    private final BibtexString oldString;
    private final BibtexString newString;

    public BibTexStringRename(BibtexString oldString, BibtexString newString, BibDatabaseContext databaseContext, DatabaseChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.oldString = oldString;
        this.newString = newString;

        setChangeName(Localization.lang("Renamed string: '%0'", oldString.getName()));
    }

    @Override
    public void applyChange(ChangeRecorder recorder) {
        if (databaseContext.getDatabase().hasStringByName(newString.getName())) {
            // The name to change to is already in the database, so we can't comply.
            LOGGER.info("Cannot rename string '{}' to '{}' because the name is already in use", oldString.getName(), newString.getName());
        }

        String currentName = oldString.getName();
        String newName = newString.getName();
        oldString.setName(newName);
        recorder.record(new UndoableStringChange(oldString, UndoableStringChange.Part.NAME, currentName, newName));
    }

    public BibtexString getOldString() {
        return oldString;
    }

    public BibtexString getNewString() {
        return newString;
    }
}
