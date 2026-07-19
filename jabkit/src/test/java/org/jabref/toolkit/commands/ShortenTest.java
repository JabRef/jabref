package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jabref.toolkit.exception.CliException;
import org.jabref.toolkit.exception.CliExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortenTest extends AbstractJabKitTest {

    @BeforeEach
    void installExceptionHandler() {
        commandLine.setExecutionExceptionHandler(new CliExceptionHandler(commandLine.getExecutionExceptionHandler()));
    }

    @Test
    void rejectsUrlInputWithUsageError() {
        // shorten needs the whole project directory; a URL downloads only a lone .tex, so it is
        // rejected before any download happens.
        int exitCode = commandLine.executeToLog("shorten", "https://example.org/paper.tex");

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("local LaTeX project directory"));
    }

    @Test
    void rejectsNonPositivePageTargetWithUsageError(@TempDir Path projectDir) throws IOException {
        Path texFile = projectDir.resolve("paper.tex");
        Files.writeString(texFile, "\\documentclass{article}\\begin{document}hi\\end{document}");

        int exitCode = commandLine.executeToLog("shorten", texFile.toString(), "--pages", "0");

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("at least 1"));
    }

    @Test
    void rejectsOutputWhenPaperReferencesMultipleBibFiles(@TempDir Path projectDir) throws IOException {
        // --output names a single destination, so a two-.bib project must be rejected up front
        // rather than silently overwriting both referenced files in place.
        Files.writeString(projectDir.resolve("a.bib"), "@Article{a1, author={X}, title={A}, year={2020}}");
        Files.writeString(projectDir.resolve("b.bib"), "@Article{b1, author={Y}, title={B}, year={2021}}");
        Path texFile = projectDir.resolve("paper.tex");
        Files.writeString(texFile, "\\documentclass{article}\\begin{document}\\cite{a1}\\bibliography{a,b}\\end{document}");

        int exitCode = commandLine.executeToLog("shorten", texFile.toString(),
                "--output", projectDir.resolve("out.bib").toString());

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("single .bib file"));
    }

    @Test
    void removesStagingDirectoryOnFailure(@TempDir Path projectDir) throws IOException {
        // A .tex without a \bibliography fails the "No bibliography file found" check *after* the
        // directory has been staged, exercising the finally-block cleanup without needing a compile.
        Path texFile = projectDir.resolve("paper.tex");
        Files.writeString(texFile, "\\documentclass{article}\\begin{document}hi\\end{document}");

        long stagingDirsBefore = countStagingDirectories();
        int exitCode = commandLine.executeToLog("shorten", texFile.toString());
        long stagingDirsAfter = countStagingDirectories();

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
        assertEquals(stagingDirsBefore, stagingDirsAfter);
    }

    @Test
    void writeBackDestinationPreservesBibSubdirectory(@TempDir Path root) throws CliException {
        Path sourceRoot = root.resolve("project");
        Path workDir = root.resolve("staging");
        Path stagedBib = workDir.resolve("bib").resolve("references.bib");

        assertEquals(sourceRoot.resolve("bib").resolve("references.bib"),
                Shorten.resolveWriteBackDestination(sourceRoot, workDir, stagedBib));
    }

    @Test
    void writeBackDestinationRefusesEscapingPath(@TempDir Path root) {
        Path sourceRoot = root.resolve("project");
        Path workDir = root.resolve("staging");
        // A bib resolved outside the staging root would map back outside the project directory.
        Path outsideBib = root.resolve("evil.bib");

        assertThrows(CliException.class,
                () -> Shorten.resolveWriteBackDestination(sourceRoot, workDir, outsideBib));
    }

    private static long countStagingDirectories() throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> entries = Files.list(tempRoot)) {
            return entries.filter(path -> path.getFileName().toString().startsWith("jabkit-shorten")).count();
        }
    }
}
