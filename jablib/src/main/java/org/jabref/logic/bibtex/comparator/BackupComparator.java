package org.jabref.logic.bibtex.comparator;

import java.io.IOException;
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
        ParsedFile current = summarize(original, preferences);
        ParsedFile candidate = summarize(backup, preferences);
        Optional<ChangeCounts> changes = current.parsed().flatMap(currentResult ->
                candidate.parsed().flatMap(candidateResult -> compareEntries(currentResult, candidateResult)));
        return new BackupComparison(current.summary(), candidate.summary(), changes);
    }

    private static ParsedFile summarize(Path path, ImportFormatPreferences preferences) throws IOException {
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
        OptionalInt entryCount = parsed.map(result -> OptionalInt.of(countNonEmptyEntries(result)))
                                       .orElseGet(OptionalInt::empty);
        return new ParsedFile(new FileSummary(path, after.size(), after.lastModifiedTime().toInstant(), entryCount), parsed);
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

    @NullMarked
    private record ParsedFile(FileSummary summary, Optional<ParserResult> parsed) {
    }
}
