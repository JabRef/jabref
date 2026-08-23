package org.jabref.model.undo;

import java.util.List;
import java.util.Objects;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;

import org.jspecify.annotations.NullMarked;

/// Insertion of entries into the library.
///
/// The entry objects themselves are retained, not their citation keys or ids: undoing a
/// removal and redoing it must put back *the same* objects, because other parts of the UI hold
/// references to them. A move to id-based lookup would break that silently, so it would have
/// to revisit this record.
///
/// `source` is reported to the model listeners. It matters because restoring entries is not
/// the same event as adding them: entries re-inserted by an undo must not be auto-assigned to
/// the group that happens to be selected, which the listener in `LibraryTab` decides by
/// looking for [EntriesEventSource#UNDO].
@NullMarked
public record UndoableInsertEntries(BibDatabase database, List<BibEntry> entries, EntriesEventSource source) implements BibChange {

    public UndoableInsertEntries {
        entries = List.copyOf(entries);
    }

    public UndoableInsertEntries(BibDatabase database, List<BibEntry> entries) {
        this(database, entries, EntriesEventSource.LOCAL);
    }

    public UndoableInsertEntries(BibDatabase database, BibEntry entry) {
        this(database, List.of(entry));
    }

    @Override
    public UndoableRemoveEntries inverted() {
        return new UndoableRemoveEntries(database, entries, source);
    }

    @Override
    public void apply() {
        database.insertEntries(entries, source);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableInsertEntries other)
                && ChangeIdentity.same(database, other.database)
                && ChangeIdentity.sameAll(entries, other.entries)
                && (source == other.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(database), ChangeIdentity.hashAll(entries), source);
    }
}
