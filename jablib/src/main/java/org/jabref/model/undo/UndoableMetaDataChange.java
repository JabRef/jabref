package org.jabref.model.undo;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;

/// Replaces a library's settings as a whole, which is how they arrive from a file rather than
/// edited field by field. Only the contents are replaced, never the [MetaData] instance, to keep
/// listeners registered on it.
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
        databaseContext.getMetaData().overwriteWith(after);
        return ApplyResult.SUCCESS;
    }
}
