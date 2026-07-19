package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jabref.toolkit.exception.CliExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static long countStagingDirectories() throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> entries = Files.list(tempRoot)) {
            return entries.filter(path -> path.getFileName().toString().startsWith("jabkit-shorten")).count();
        }
    }
}
