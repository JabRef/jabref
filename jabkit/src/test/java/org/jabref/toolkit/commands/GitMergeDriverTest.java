package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Covers the Git merge driver contract: exit codes, the output on stderr, and the file the
/// result goes to. The merge itself is tested in `BibFileMergerTest`.
class GitMergeDriverTest extends AbstractJabKitTest {

    @TempDir
    private Path tempDir;

    /// The first localized message otherwise comes with a warning that the language is not set
    @BeforeAll
    static void setLanguage() {
        Localization.setLanguage(Language.ENGLISH);
    }

    @Test
    void mergedFileYieldsExitCodeZero() throws IOException {
        Path base = mergeFile("base", "merge-base.bib");
        Path current = mergeFile("current", "merge-current.bib");
        Path other = mergeFile("other", "merge-other.bib");

        int exitCode = commandLine.executeToLog("git", "merge-driver", base.toString(), current.toString(), other.toString());

        assertEquals(0, exitCode, commandLine.getErrorOutput());
        assertEquals("Merged " + current + " without conflicts.\n", commandLine.getStandardOutput());
        assertTrue(Files.readString(current).contains("Newton1999"));
    }

    @Test
    void conflictsAreListedOnStderrWithExitCodeOne() throws IOException {
        Path base = mergeFile("base", "merge-base.bib");
        Path current = mergeFile("current", "merge-current.bib");
        Path other = mergeFile("other", "merge-other-conflict.bib");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(1, exitCode);
        assertEquals("""
                1 entries could not be merged automatically:
                  Smith2020: changed on both sides with different content
                """, commandLine.getErrorOutput());
        assertEquals("", commandLine.getStandardOutput());
    }

    @Test
    void refusedMergeLeavesCurrentUntouchedWithExitCodeOne() throws IOException {
        Path base = mergeFile("base", "merge-base.bib");
        Path current = mergeFile("current", "merge-duplicate-keys.bib");
        Path other = mergeFile("other", "merge-other.bib");

        int exitCode = commandLine.executeToLog("git", "merge-driver", "--porcelain", base.toString(), current.toString(), other.toString());

        assertEquals(1, exitCode);
        assertEquals("Cannot merge " + current + ": citation keys must be unique.\n", commandLine.getErrorOutput());
        assertEquals(Files.readString(getClassResourceAsPath("merge-duplicate-keys.bib")), Files.readString(current));
    }

    @Test
    void gitWithoutSubcommandIsUsageError() {
        int exitCode = commandLine.executeToLog("git");

        assertEquals(2, exitCode);
    }

    /// Git hands the driver temporary files without a `.bib` extension
    private Path mergeFile(String name, String resource) throws IOException {
        return Files.copy(getClassResourceAsPath(resource), tempDir.resolve(".merge_file_" + name));
    }
}
