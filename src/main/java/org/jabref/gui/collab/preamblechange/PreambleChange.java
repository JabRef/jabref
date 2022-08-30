package org.jabref.gui.collab.preamblechange;

import org.jabref.gui.collab.ExternalChange;
import org.jabref.gui.collab.ExternalChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoablePreambleChange;
import org.jabref.logic.bibtex.comparator.PreambleDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PreambleChange extends ExternalChange {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreambleChange.class);

    private final PreambleDiff preambleDiff;

    public PreambleChange(PreambleDiff preambleDiff, BibDatabaseContext databaseContext, ExternalChangeResolverFactory externalChangeResolverFactory) {
        super(databaseContext, externalChangeResolverFactory);
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
