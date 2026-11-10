package org.jabref.logic.bibtex.comparator;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.OptionalInt;

import org.jabref.logic.bibtex.comparator.BackupComparison.ChangeCounts;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class BackupComparator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupComparator.class);

    private BackupComparator() {
    }

    /// Compares the library with its backup.
    ///
    /// @throws InterruptedIOException if the calling thread is interrupted
    public static BackupComparison compare(Path original, Path backup, ImportFormatPreferences preferences) throws IOException {
        stopIfInterrupted();
        Optional<ParserResult> current = parse(original, preferences);
        stopIfInterrupted();
        Optional<ParserResult> candidate = parse(backup, preferences);
        stopIfInterrupted();
        Optional<ChangeCounts> changes = current.flatMap(currentResult ->
                candidate.flatMap(candidateResult -> compareEntries(currentResult, candidateResult)));
        return new BackupComparison(entryCount(current), entryCount(candidate), changes);
    }

    private static void stopIfInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Backup comparison was cancelled");
        }
    }

    private static Optional<ParserResult> parse(Path path, ImportFormatPreferences preferences) throws IOException {
        BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class);
        if (!before.isRegularFile()) {
            throw new IOException("Not a regular file: " + path);
        }

        Optional<ParserResult> parsed = Optional.empty();
        try {
            ParserResult result = OpenDatabase.loadDatabase(path, preferences, new DummyFileUpdateMonitor());
            if (canCompareEntries(result)) {
                parsed = Optional.of(result);
            }
        } catch (IOException e) {
            LOGGER.debug("Could not parse backup comparison file {}", path, e);
        }

        BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class);
        if (before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime())) {
            parsed = Optional.empty();
        }
        return parsed;
    }

    private static OptionalInt entryCount(Optional<ParserResult> parsed) {
        return parsed.map(result -> OptionalInt.of(countNonEmptyEntries(result)))
                     .orElseGet(OptionalInt::empty);
    }

    private static int countNonEmptyEntries(ParserResult result) {
        return (int) result.getDatabase().getEntries().stream()
                           .filter(entry -> !entry.isEmpty())
                           .count();
    }

    private static boolean canCompareEntries(ParserResult result) {
        if (result.isInvalid()) {
            return false;
        }

        // Duplicate string definitions do not discard entries. Other warnings may indicate skipped entries.
        return result.warnings().stream().allMatch(warning ->
                result.getDatabase().getStringValues().stream().anyMatch(string ->
                        warning.equals(Localization.lang("Duplicate string name: '%0'", string.getName()))));
    }

    private static Optional<ChangeCounts> compareEntries(ParserResult current, ParserResult candidate) {
        int currentCount = countNonEmptyEntries(current);
        int backupCount = countNonEmptyEntries(candidate);
        int added = 0;
        int removed = 0;
        int modified = 0;
        for (BibEntryDiff difference : BibDatabaseDiff.compare(current.getDatabaseContext(), candidate.getDatabaseContext()).getEntryDifferences()) {
            if (difference.originalEntry() == null) {
                added++;
            } else if (difference.newEntry() == null) {
                removed++;
            } else {
                modified++;
            }
        }
        if (currentCount - removed + added != backupCount) {
            return Optional.empty();
        }
        return Optional.of(new ChangeCounts(added, removed, modified));
    }
}
