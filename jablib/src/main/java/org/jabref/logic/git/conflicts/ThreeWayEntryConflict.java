package org.jabref.logic.git.conflicts;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Represents a semantic conflict between base, local, and remote versions of a {@link BibEntry}.
/// This is similar in structure to {@link RevisionTriple}, but uses nullable entries to model deletion.
///
/// Constraint: At least one of {@code local} or {@code remote} must be non-null.
@NullMarked
public record ThreeWayEntryConflict(
        @Nullable BibEntry base,
        @Nullable BibEntry local,
        @Nullable BibEntry remote
) {
    public ThreeWayEntryConflict {
        assert !(local == null && remote == null) : "Both local and remote are null: conflict must involve at least one side.";
    }
}
