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
/// Verifying against the recorded state would refuse the undo outright whenever any unrelated
/// metadata write had happened, and per-setting records were rejected because seven tabs write
/// straight into one instance.
///
/// What no dialog edits is not taken back either: the AI library id, which the chat creates
/// lazily and which keys the stored chat history, and the `.blg` paths, which the integrity
/// check writes. Both are copied from the live instance before the snapshot is installed, so
/// undoing the properties dialog after opening the chat does not orphan the chats.
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
        MetaData live = databaseContext.getMetaData();
        MetaData target = MetaData.copyOf(after);
        live.getAiLibraryId().ifPresent(target::setAiLibraryId);
        target.getBlgFilePaths().clear();
        target.getBlgFilePaths().putAll(live.getBlgFilePaths());
        live.overwriteWith(target, MetaDataChangeSource.JOURNAL);
        return ApplyResult.SUCCESS;
    }
}
