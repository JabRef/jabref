package org.jabref.toolkit.commands;

import org.jabref.toolkit.exception.CliExceptionHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
