package org.jabref.model.change;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Insertion of entries into the library.
///
/// The entry objects themselves are retained, not their citation keys or ids: undoing a
/// removal and redoing it must put back *the same* objects, because other parts of the UI hold
/// references to them. A move to id-based lookup would break that silently, so it would have
/// to revisit this record.
@NullMarked
public record EntriesInserted(List<BibEntry> entries) implements BibChange {

    public EntriesInserted {
        entries = List.copyOf(entries);
    }

    public EntriesInserted(BibEntry entry) {
        this(List.of(entry));
    }

    @Override
    public EntriesRemoved inverted() {
        return new EntriesRemoved(entries);
    }

    @Override
    public void applyTo(BibDatabaseContext context) {
        context.getDatabase().insertEntries(entries);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof EntriesInserted other) && ChangeIdentity.sameAll(entries, other.entries);
    }

    @Override
    public int hashCode() {
        return ChangeIdentity.hashAll(entries);
    }
}
