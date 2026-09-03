package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitMergeDriverTest extends AbstractJabKitTest {

    private static final BibEntry SMITH_BASE = new BibEntry(StandardEntryType.Article)
            .withCitationKey("Smith2020")
            .withField(StandardField.AUTHOR, "Smith, John")
            .withField(StandardField.TITLE, "Base Title")
            .withField(StandardField.YEAR, "2020");

    private static final BibEntry DOE = new BibEntry(StandardEntryType.Article)
            .withCitationKey("Doe2021")
            .withField(StandardField.AUTHOR, "Doe, Jane")
            .withField(StandardField.TITLE, "Another Paper")
            .withField(StandardField.YEAR, "2021");

    private static final BibEntry NEWTON = new BibEntry(StandardEntryType.Book)
            .withCitationKey("Newton1999")
            .withField(StandardField.AUTHOR, "Newton, Isaac")
            .withField(StandardField.TITLE, "The Principia")
            .withField(StandardField.YEAR, "1999");

    /// Git hands the driver temporary files without a `.bib` extension
    private Path copyToMergeFile(Path source, Path tempDir, String name) throws IOException {
        Path target = tempDir.resolve(".merge_file_" + name);
        Files.copy(source, target);
        return target;
    }

    private List<BibEntry> parse(Path file) throws IOException {
        return new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor())
                .importDatabase(file).getDatabase().getEntries();
    }

    @Test
    void appliesNonConflictingChangesToCurrent(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", base.toString(), current.toString(), other.toString());

        assertEquals(0, exitCode, commandLine.getErrorOutput());
        BibEntry smith = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Current Title")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.JOURNAL, "Journal of Tests")
                .withField(StandardField.DOI, "10.1000/xyz123");
        assertEquals(List.of(smith, DOE, NEWTON), parse(current));
    }

    @Test
    void reportsConflictAndKeepsCurrentVersion(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other-conflict.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(1, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("Smith2020"), commandLine.getErrorOutput());
        BibEntry smith = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2020")
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Current Title")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.JOURNAL, "Journal of Tests");
        assertEquals(List.of(smith, DOE), parse(current));
    }

    @Test
    void identicalSidesMergeCleanly(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(0, exitCode, commandLine.getErrorOutput());
        assertEquals(List.of(SMITH_BASE, DOE), parse(current));
    }

    @Test
    void appliesEntryTypeChangeFromOther(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other-type.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(0, exitCode, commandLine.getErrorOutput());
        BibEntry smith = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Smith2020")
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Current Title")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.JOURNAL, "Journal of Tests");
        assertEquals(List.of(smith, DOE, NEWTON), parse(current));
    }

    @Test
    void keepsCurrentTypeAndMergesOtherEntriesOnDivergingEntryTypes(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current-type.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other-type.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(1, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("Smith2020"), commandLine.getErrorOutput());
        BibEntry smith = new BibEntry(StandardEntryType.Report)
                .withCitationKey("Smith2020")
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Base Title")
                .withField(StandardField.YEAR, "2020");
        assertEquals(List.of(smith, DOE, NEWTON), parse(current));
    }

    @Test
    void keepsEntryTypeChangedInCurrentAndDeletedInOther(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current-type.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other-deleted.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(1, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("Smith2020"), commandLine.getErrorOutput());
        BibEntry smith = new BibEntry(StandardEntryType.Report)
                .withCitationKey("Smith2020")
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Base Title")
                .withField(StandardField.YEAR, "2020");
        assertEquals(List.of(smith, DOE), parse(current));
    }

    @Test
    void reportsConflictOnStringChangeInOther(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other-string.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(1, exitCode);
        assertEquals(Files.readString(getClassResourceAsPath("merge-current.bib")), Files.readString(current));
    }

    @Test
    void reportsConflictOnDuplicateCitationKeys(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-duplicate-keys.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(1, exitCode);
        assertEquals(Files.readString(getClassResourceAsPath("merge-duplicate-keys.bib")), Files.readString(current));
    }

    @Test
    void gitWithoutSubcommandFails() {
        int exitCode = commandLine.executeToLog("git");

        assertEquals(2, exitCode);
    }
}
