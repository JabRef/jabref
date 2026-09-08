package org.jabref.model.undo;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.event.MetaDataChangeSource;

import org.jspecify.annotations.NullMarked;

/// Replaces a library's settings as a whole, which is how they arrive from a file, and how the
/// library properties dialog writes them. Only the contents are replaced, never the [MetaData]
/// instance, to keep listeners registered on it.
///
/// Applies unconditionally, as every change describing a collection does — see [BibChange#apply].
/// The cost is that a setting written by someone else between recording and undoing is replaced
/// along with the rest: undoing the properties dialog after an AI library id or a `.blg` path was
/// written takes that write with it. Verifying instead would refuse the undo outright whenever any
/// unrelated metadata write had happened, which is the worse of the two, and per-setting records
/// were rejected because seven tabs write straight into one instance.
@NullMarked
public record UndoableMetaDataChange(BibDatabaseContext databaseContext, MetaData before, MetaData after) implements BibChange {

    public UndoableMetaDataChange {
        before = MetaData.copyOf(before);
        after = MetaData.copyOf(after);
    }

    @Override
    public UndoableMetaDataChange inverted() {
        return new UndoableMetaDataChange(databaseContext, after, before);
    }

    @Override
    public ApplyResult apply() {
        databaseContext.getMetaData().overwriteWith(after, MetaDataChangeSource.JOURNAL);
        return ApplyResult.SUCCESS;
    }
}
