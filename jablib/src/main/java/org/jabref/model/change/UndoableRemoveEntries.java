package org.jabref.model.change;

import java.util.List;
import java.util.Objects;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;

import org.jspecify.annotations.NullMarked;

/// Removal of entries from the library. See [UndoableInsertEntries] on why the entry objects are
/// retained rather than looked up again on undo.
///
/// `reinsertSource` is the event source the inverse change reports when it puts the entries
/// back. It defaults to [EntriesEventSource#UNDO], because restoring removed entries must not
/// auto-assign them to the currently selected group. Removal itself is always reported as a
/// local change.
@NullMarked
public record UndoableRemoveEntries(BibDatabase database, List<BibEntry> entries, EntriesEventSource reinsertSource) implements BibChange {

    public UndoableRemoveEntries {
        entries = List.copyOf(entries);
    }

    public UndoableRemoveEntries(BibDatabase database, List<BibEntry> entries) {
        this(database, entries, EntriesEventSource.UNDO);
    }

    public UndoableRemoveEntries(BibDatabase database, BibEntry entry) {
        this(database, List.of(entry));
    }

    @Override
    public UndoableInsertEntries inverted() {
        return new UndoableInsertEntries(database, entries, reinsertSource);
    }

    @Override
    public void apply() {
        database.removeEntries(entries);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableRemoveEntries other)
                && ChangeIdentity.same(database, other.database)
                && ChangeIdentity.sameAll(entries, other.entries)
                && (reinsertSource == other.reinsertSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(database), ChangeIdentity.hashAll(entries), reinsertSource);
    }
}
