package org.jabref.gui.collab.metedatachange;

import org.jabref.gui.collab.ExternalChange;
import org.jabref.gui.collab.ExternalChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.bibtex.comparator.MetaDataDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

public final class MetadataChange extends ExternalChange {
    private final MetaDataDiff metaDataDiff;

    public MetadataChange(MetaDataDiff metaDataDiff, BibDatabaseContext databaseContext, ExternalChangeResolverFactory externalChangeResolverFactory) {
        super(databaseContext, externalChangeResolverFactory);
        this.metaDataDiff = metaDataDiff;
        setChangeName(Localization.lang("Metadata change"));
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        // TODO: Metadata edit should be undoable
        databaseContext.setMetaData(metaDataDiff.getNewMetaData());
        // group change is handled by GroupChange, so we set the groups root to the original value
        // to prevent any inconsistency
        metaDataDiff.getGroupDifferences()
                    .ifPresent(groupDiff -> databaseContext.getMetaData().setGroups(groupDiff.getOriginalGroupRoot()));
    }

    public MetaDataDiff getMetaDataDiff() {
        return metaDataDiff;
    }
}
