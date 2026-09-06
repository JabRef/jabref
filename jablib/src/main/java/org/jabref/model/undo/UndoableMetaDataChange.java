package org.jabref.model.undo;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;

/// Replaces a library's metadata as a whole, which is how the library's own settings — mode,
/// encoding, citation key pattern, file directories, save actions, content selectors, protection —
/// arrive when they are read from a file rather than edited field by field.
@NullMarked
public record UndoableMetaDataChange(BibDatabaseContext databaseContext, MetaData before, MetaData after) implements BibChange {

    @Override
    public UndoableMetaDataChange inverted() {
        return new UndoableMetaDataChange(databaseContext, after, before);
    }

    @Override
    public void apply() {
        databaseContext.setMetaData(after);
    }
}
