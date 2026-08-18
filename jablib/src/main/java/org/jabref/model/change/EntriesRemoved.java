package org.jabref.model.change;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Removal of entries from the library. See [EntriesInserted] on why the entry objects are
/// retained rather than looked up again on undo.
@NullMarked
public record EntriesRemoved(List<BibEntry> entries) implements BibChange {

    public EntriesRemoved {
        entries = List.copyOf(entries);
    }

    public EntriesRemoved(BibEntry entry) {
        this(List.of(entry));
    }

    @Override
    public EntriesInserted inverted() {
        return new EntriesInserted(entries);
    }

    @Override
    public void applyTo(BibDatabaseContext context) {
        context.getDatabase().removeEntries(entries);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof EntriesRemoved other) && ChangeIdentity.sameAll(entries, other.entries);
    }

    @Override
    public int hashCode() {
        return ChangeIdentity.hashAll(entries);
    }
}
