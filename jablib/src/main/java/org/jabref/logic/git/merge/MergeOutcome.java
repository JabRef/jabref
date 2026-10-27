package org.jabref.logic.git.merge;

import java.util.List;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;

import org.jspecify.annotations.NullMarked;

/// What [BibFileMerger] did with the three versions of a file.
@NullMarked
public sealed interface MergeOutcome {

    /// Nothing was written, because writing would have lost content.
    record Refused(List<Refusal> refusals) implements MergeOutcome {
    }

    /// The `current` file holds the merge result. The listed entries kept their `current`
    /// version, because both sides changed them in different ways.
    record Merged(List<ThreeWayEntryConflict> conflicts) implements MergeOutcome {
    }
}
