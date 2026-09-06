package org.jabref.model.undo;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NullMarked;

/// Replaces the contents of a library's metadata as a whole, which is how the library's own settings —
/// mode, encoding, citation key pattern, file directories, save actions, content selectors, protection —
/// arrive when they are read from a file rather than edited field by field.
///
/// The metadata *instance* the library holds is never replaced: listeners are registered on it
/// ([org.jabref.model.metadata.MetaData#registerListener]), and swapping it would leave them
/// subscribed to a discarded object.
@NullMarked
public record UndoableMetaDataChange(BibDatabaseContext databaseContext, MetaData before, MetaData after) implements BibChange {

    /// Both states are copied, because applying writes into the library's live metadata instance and
    /// that instance is typically the `before` state. Holding it by reference would mean applying this
    /// change overwrites the very state it has to be able to restore.
    public UndoableMetaDataChange {
        before = copyOf(before);
        after = copyOf(after);
    }

    @Override
    public UndoableMetaDataChange inverted() {
        return new UndoableMetaDataChange(databaseContext, after, before);
    }

    @Override
    public void apply() {
        databaseContext.getMetaData().setContentsFrom(after);
    }

    private static MetaData copyOf(MetaData metaData) {
        MetaData copy = new MetaData();
        copy.setContentsFrom(metaData);
        return copy;
    }
}
