package org.jabref.gui.collab.preamblechange;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoablePreambleChange;
import org.jabref.logic.bibtex.comparator.PreambleDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PreambleChange extends DatabaseChange {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreambleChange.class);

    private final PreambleDiff preambleDiff;

    public PreambleChange(PreambleDiff preambleDiff, BibDatabaseContext databaseContext, DatabaseChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.preambleDiff = preambleDiff;

        setChangeName(Localization.lang("Changed preamble"));
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        databaseContext.getDatabase().setPreamble(preambleDiff.getNewPreamble());
        undoEdit.addEdit(new UndoablePreambleChange(databaseContext.getDatabase(), preambleDiff.getOriginalPreamble(), preambleDiff.getNewPreamble()));
    }

    public PreambleDiff getPreambleDiff() {
        return preambleDiff;
    }
}
