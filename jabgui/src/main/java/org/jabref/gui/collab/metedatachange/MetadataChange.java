package org.jabref.gui.collab.metedatachange;

import java.util.Optional;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.logic.bibtex.comparator.MetaDataDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoableMetaDataChange;

public final class MetadataChange extends DatabaseChange {
    private final MetaDataDiff metaDataDiff;

    public MetadataChange(MetaDataDiff metaDataDiff, BibDatabaseContext databaseContext, DatabaseChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.metaDataDiff = metaDataDiff;
        setChangeName(Localization.lang("Metadata change"));
    }

    @Override
    public void applyChange(CompoundEdit undoEdit) {
        MetaData newMetaData = metaDataDiff.getNewMetaData();
        // group change is handled by GroupChange, so the groups root keeps its original value
        // to prevent any inconsistency - a library without groups stays without groups
        metaDataDiff.getGroupDifferences()
                    .ifPresent(groupDiff -> Optional.ofNullable(groupDiff.getOriginalGroupRoot())
                                                    .ifPresentOrElse(newMetaData::setGroups, newMetaData::clearGroups));

        undoEdit.applyEdit(new UndoableMetaDataChange(databaseContext, databaseContext.getMetaData(), newMetaData));
    }

    public MetaDataDiff getMetaDataDiff() {
        return metaDataDiff;
    }
}
