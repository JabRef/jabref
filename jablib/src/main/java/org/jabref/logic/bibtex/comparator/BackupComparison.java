package org.jabref.logic.bibtex.comparator;

import java.util.Optional;
import java.util.OptionalInt;

import org.jspecify.annotations.NullMarked;

/// The backup's content is compared with the current file. Missing entry counts or changes mean the content could not
/// be compared reliably; they do not mean zero entries or zero changes.
@NullMarked
public record BackupComparison(OptionalInt currentEntryCount, OptionalInt backupEntryCount, Optional<ChangeCounts> changes) {
    /// Changes needed to turn the current library into the backup library. `otherContentChanged` covers differences
    /// outside the entries themselves: groups and other metadata, string definitions, and the preamble.
    @NullMarked
    public record ChangeCounts(int added, int removed, int modified, boolean otherContentChanged) {
    }
}
