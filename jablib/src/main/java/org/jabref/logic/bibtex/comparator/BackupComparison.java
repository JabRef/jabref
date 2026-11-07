package org.jabref.logic.bibtex.comparator;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;

import org.jspecify.annotations.NullMarked;

/// The backup's content is compared with the current file. Missing entry counts or changes mean the content could not
/// be compared reliably; they do not mean zero entries or zero changes.
@NullMarked
public record BackupComparison(FileSummary current, FileSummary backup, Optional<ChangeCounts> changes) {
    /// Raw file metadata. The timestamp is the file system's last-modified time, not a parsed backup filename.
    /// The entry count excludes empty entries, as does [BibDatabaseDiff].
    @NullMarked
    public record FileSummary(Path path, long sizeBytes, Instant lastModifiedAt, OptionalInt entryCount) {
    }

    /// Changes needed to turn the current library into the backup library.
    @NullMarked
    public record ChangeCounts(int added, int removed, int modified) {
    }
}
