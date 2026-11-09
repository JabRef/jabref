package org.jabref.logic.bibtex.comparator;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.OptionalInt;

import org.jabref.logic.bibtex.comparator.BackupComparison.ChangeCounts;
import org.jabref.logic.bibtex.comparator.BackupComparison.FileSummary;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class BackupComparator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupComparator.class);

    private BackupComparator() {
    }

    /// Compares the explicitly selected files. An entry count is absent when its file cannot be parsed reliably;
    /// changes are absent when either file cannot be compared reliably.
    /// Added and removed counts describe the backup relative to the current library.
    public static BackupComparison compare(Path original, Path backup, ImportFormatPreferences preferences) throws IOException {
        return compareAndKeepParsedFiles(original, backup, preferences, new DummyFileUpdateMonitor()).comparison();
    }

    /// Like [#compare(Path, Path, ImportFormatPreferences)], but also returns the parsed files, so that a caller that
    /// needs them afterwards (for example to review the backup) does not have to parse both files again.
    ///
    /// The comparison stops between its steps when the calling thread is interrupted, e.g. because the task that
    /// opens the library was cancelled.
    ///
    /// @param originalFileMonitor the monitor to parse the original with, as the parsed original may be opened
    /// @throws InterruptedIOException if the calling thread was interrupted
    public static ParsedComparison compareAndKeepParsedFiles(Path original, Path backup, ImportFormatPreferences preferences, FileUpdateMonitor originalFileMonitor) throws IOException {
        stopIfInterrupted();
        ParsedFile current = summarize(original, preferences, originalFileMonitor);
        stopIfInterrupted();
        ParsedFile candidate = summarize(backup, preferences, new DummyFileUpdateMonitor());
        stopIfInterrupted();
        Optional<ChangeCounts> changes = current.comparable().flatMap(currentResult ->
                candidate.comparable().flatMap(candidateResult -> compareEntries(currentResult, candidateResult)));
        return new ParsedComparison(
                new BackupComparison(current.summary(), candidate.summary(), changes),
                current.loaded(),
                candidate.loaded());
    }

    private static void stopIfInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Backup comparison was cancelled");
        }
    }

    private static ParsedFile summarize(Path path, ImportFormatPreferences preferences, FileUpdateMonitor fileMonitor) throws IOException {
        BasicFileAttributes before = Files.readAttributes(path, BasicFileAttributes.class);
        if (!before.isRegularFile()) {
            throw new IOException("Not a regular file: " + path);
        }

        Optional<ParserResult> loaded = Optional.empty();
        try {
            loaded = Optional.of(OpenDatabase.loadDatabase(path, preferences, fileMonitor));
        } catch (IOException e) {
            LOGGER.debug("Could not parse backup comparison file {}", path, e);
        }

        BasicFileAttributes after = Files.readAttributes(path, BasicFileAttributes.class);
        if (before.size() != after.size() || !before.lastModifiedTime().equals(after.lastModifiedTime())) {
            loaded = Optional.empty();
        }
        Optional<ParserResult> comparable = loaded.filter(BackupComparator::canCompareEntries);
        OptionalInt entryCount = comparable.map(result -> OptionalInt.of(countNonEmptyEntries(result)))
                                           .orElseGet(OptionalInt::empty);
        return new ParsedFile(new FileSummary(path, after.size(), after.lastModifiedTime().toInstant(), entryCount), loaded, comparable);
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

    /// The comparison together with the files it parsed. A parsed file is absent when it could not be read or changed
    /// while it was read. It is present even when it could not be parsed reliably (e.g. it is marked invalid), as the
    /// caller decides how to handle such a file.
    @NullMarked
    public record ParsedComparison(BackupComparison comparison, Optional<ParserResult> current, Optional<ParserResult> backup) {
    }

    /// `loaded` is the parse of the file; `comparable` is that parse only if its entries can be counted reliably.
    @NullMarked
    private record ParsedFile(FileSummary summary, Optional<ParserResult> loaded, Optional<ParserResult> comparable) {
    }
}
