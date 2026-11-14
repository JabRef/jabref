package org.jabref.logic.bibtex.comparator;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalInt;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class BackupComparatorTest {
    private final ImportFormatPreferences preferences = createPreferences();

    @Test
    void comparesIdenticalEntries(@TempDir Path tempDir) throws IOException {
        String content = "@Article{same, title = {Same}}";
        Path original = Files.writeString(tempDir.resolve("library.bib"), content);
        Path backup = Files.writeString(tempDir.resolve("backup.bib"), content);

        BackupComparison result = BackupComparator.compare(original, backup, preferences);

        assertEquals(OptionalInt.of(1), result.currentEntryCount());
        assertEquals(OptionalInt.of(1), result.backupEntryCount());
        assertEquals(Optional.of(new BackupComparison.ChangeCounts(0, 0, 0, false)), result.changes());
    }

    @Test
    void reportsBackupEntryAddedRelativeToCurrent(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "");
        Path backup = Files.writeString(tempDir.resolve("backup.bib"), "@Article{added, title = {Added}}");

        BackupComparison result = BackupComparator.compare(original, backup, preferences);

        assertEquals(OptionalInt.of(0), result.currentEntryCount());
        assertEquals(OptionalInt.of(1), result.backupEntryCount());
        assertEquals(Optional.of(new BackupComparison.ChangeCounts(1, 0, 0, false)), result.changes());
    }

    @Test
    void reportsBackupEntryRemovedWhenBackupIsEmpty(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{removed, title = {Removed}}");
        Path backup = Files.writeString(tempDir.resolve("backup.bib"), "");

        BackupComparison result = BackupComparator.compare(original, backup, preferences);

        assertEquals(OptionalInt.of(0), result.backupEntryCount());
        assertEquals(Optional.of(new BackupComparison.ChangeCounts(0, 1, 0, false)), result.changes());
    }

    @Test
    void reportsBackupEntryRemovedWhenAnotherEntryRemains(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"),
                "@Article{keep, title = {Kept}}\n@Article{removed, title = {Removed}}");
        Path backup = Files.writeString(tempDir.resolve("backup.bib"), "@Article{keep, title = {Kept}}");

        BackupComparison result = BackupComparator.compare(original, backup, preferences);

        assertEquals(OptionalInt.of(2), result.currentEntryCount());
        assertEquals(OptionalInt.of(1), result.backupEntryCount());
        assertEquals(Optional.of(new BackupComparison.ChangeCounts(0, 1, 0, false)), result.changes());
    }

    @Test
    void reportsChangedEntryWithSameCitationKey(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{same, title = {Original}}");
        Path backup = Files.writeString(tempDir.resolve("backup.bib"), "@Article{same, title = {Changed}}");

        BackupComparison result = BackupComparator.compare(original, backup, preferences);

        assertEquals(Optional.of(new BackupComparison.ChangeCounts(0, 0, 1, false)), result.changes());
    }

    @Test
    void comparesEntriesDespiteDuplicateStringWarning(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), """
                @String{journal = {First}
                }
                @String{journal = {Second}
                }
                @Article{keep, title = {Kept}
                }
                """);
        Path backup = Files.writeString(tempDir.resolve("backup.bib"), """
                @Article{keep, title = {Kept}
                }
                @Article{added, title = {Added}
                }
                """);

        ParserResult parsed = OpenDatabase.loadDatabase(original, preferences, new DummyFileUpdateMonitor());
        assertTrue(parsed.hasWarnings());
        assertFalse(parsed.isInvalid());

        BackupComparison result = BackupComparator.compare(original, backup, preferences);

        assertEquals(OptionalInt.of(1), result.currentEntryCount());
        assertEquals(OptionalInt.of(2), result.backupEntryCount());
        // The current file defines @String entries the backup does not, so string definitions differ.
        assertEquals(Optional.of(new BackupComparison.ChangeCounts(1, 0, 0, true)), result.changes());
    }

    /// A file that parses with warnings (here an entry the parser skips) is still compared: the counts are only a
    /// hint, and "Review backup" shows the real diff. Only an invalid parse stops the comparison.
    @Test
    void comparesFileThatParsesWithWarnings(@TempDir Path tempDir) throws IOException {
        String content = "@Article{broken, author=\"author {missing bracket\"}";
        Path original = Files.writeString(tempDir.resolve("library.bib"), content);
        Path backup = Files.writeString(tempDir.resolve("backup.bib"), "@Article{good, title = {Good}}");

        ParserResult parsed = OpenDatabase.loadDatabase(original, preferences, new DummyFileUpdateMonitor());
        assertFalse(parsed.isInvalid());
        assertTrue(parsed.hasWarnings());
        assertEquals(0, parsed.getDatabase().getEntryCount());

        BackupComparison result = BackupComparator.compare(original, backup, preferences);

        assertEquals(OptionalInt.of(0), result.currentEntryCount());
        assertEquals(OptionalInt.of(1), result.backupEntryCount());
        assertEquals(Optional.of(new BackupComparison.ChangeCounts(1, 0, 0, false)), result.changes());
    }

    @Test
    void ignoresEmptyEntriesWhenCountingChanges(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), """
                @Misc{,
                  timestamp = {2026-10-22}
                }
                @Article{keep,
                  title = {Kept}
                }
                @Article{removed,
                  title = {Removed}
                }
                """);
        Path backup = Files.writeString(tempDir.resolve("backup.bib"), "@Article{keep, title = {Kept}}");

        BackupComparison result = BackupComparator.compare(original, backup, preferences);

        assertEquals(OptionalInt.of(2), result.currentEntryCount());
        assertEquals(OptionalInt.of(1), result.backupEntryCount());
        assertEquals(Optional.of(new BackupComparison.ChangeCounts(0, 1, 0, false)), result.changes());
    }

    @Test
    void doesNotReportMisleadingCountsForUnparseableOriginal(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), """
                <<<<<<< HEAD
                @Article{first,
                  title = {First},
                }
                =======
                @Article{second,
                  title = {Second},
                }
                >>>>>>> branch
                """);
        Path backup = Files.writeString(tempDir.resolve("backup.bib"), "@Article{backup, title = {Backup}}");

        BackupComparison result = BackupComparator.compare(original, backup, preferences);

        assertEquals(OptionalInt.empty(), result.currentEntryCount());
        assertEquals(OptionalInt.of(1), result.backupEntryCount());
        assertEquals(Optional.empty(), result.changes());
    }

    @Test
    void missingBackupIsReportedToCaller(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{original}");
        Path missingBackup = tempDir.resolve("missing.bib");

        assertThrows(NoSuchFileException.class, () -> BackupComparator.compare(original, missingBackup, preferences));
    }

    @Test
    void directoryIsNotAcceptedAsBackup(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{original}");
        Path directory = Files.createDirectory(tempDir.resolve("backup.bib"));

        assertThrows(IOException.class, () -> BackupComparator.compare(original, directory, preferences));
    }

    // [utest->req~jabgui.backup.restore-dialog-non-blocking~2]
    @Test
    void interruptedThreadStopsComparison(@TempDir Path tempDir) throws IOException {
        Path original = Files.writeString(tempDir.resolve("library.bib"), "@Article{original}");
        Path backup = Files.writeString(tempDir.resolve("backup.bib"), "@Article{backup}");

        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedIOException.class, () -> BackupComparator.compare(original, backup, preferences));
        } finally {
            Thread.interrupted();
        }
    }

    private static ImportFormatPreferences createPreferences() {
        ImportFormatPreferences preferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.bibEntryPreferences().getImportKeywordDelimiters()).thenReturn(";,");
        return preferences;
    }
}
