package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitMergeDriverTest extends AbstractJabKitTest {

    /// Git hands the driver temporary files without a `.bib` extension
    private Path copyToMergeFile(Path source, Path tempDir, String name) throws IOException {
        Path target = tempDir.resolve(".merge_file_" + name);
        Files.copy(source, target);
        return target;
    }

    @Test
    void appliesNonConflictingChangesToCurrent(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", base.toString(), current.toString(), other.toString());

        assertEquals(0, exitCode, commandLine.getErrorOutput());
        String merged = Files.readString(current);
        // kept from current
        assertTrue(merged.contains("Current Title"));
        assertTrue(merged.contains("Journal of Tests"));
        // taken from other
        assertTrue(merged.contains("10.1000/xyz123"));
        assertTrue(merged.contains("Newton1999"));
    }

    @Test
    void reportsConflictAndKeepsCurrentVersion(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other-conflict.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(1, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("Smith2020"));
        String merged = Files.readString(current);
        assertTrue(merged.contains("Current Title"));
        assertFalse(merged.contains("Conflicting Title"));
    }

    @Test
    void identicalSidesMergeCleanly(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(0, exitCode, commandLine.getErrorOutput());
        assertTrue(Files.readString(current).contains("Smith2020"));
    }

    @Test
    void appliesEntryTypeChangeFromOther(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other-type.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(0, exitCode, commandLine.getErrorOutput());
        String merged = Files.readString(current);
        assertTrue(merged.contains("@Book{Smith2020,"));
        assertTrue(merged.contains("Current Title"));
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
    void reportsConflictOnDivergingEntryTypes(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current-type.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other-type.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(1, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("Smith2020"));
        assertEquals(Files.readString(getClassResourceAsPath("merge-current-type.bib")), Files.readString(current));
    }

    @Test
    void reportsConflictOnEntryTypeChangedAndDeleted(@TempDir Path tempDir) throws IOException {
        Path base = copyToMergeFile(getClassResourceAsPath("merge-base.bib"), tempDir, "base");
        Path current = copyToMergeFile(getClassResourceAsPath("merge-current-type.bib"), tempDir, "current");
        Path other = copyToMergeFile(getClassResourceAsPath("merge-other-deleted.bib"), tempDir, "other");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(1, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("Smith2020"));
        assertEquals(Files.readString(getClassResourceAsPath("merge-current-type.bib")), Files.readString(current));
    }

    @Test
    void gitWithoutSubcommandFails() {
        int exitCode = commandLine.executeToLog("git");

        assertEquals(2, exitCode);
    }
}
