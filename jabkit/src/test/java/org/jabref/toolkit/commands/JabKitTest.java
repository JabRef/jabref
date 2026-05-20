package org.jabref.toolkit.commands;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Collects various tests
/// Better use a more specific test class per command. See [SearchTest] for an example.
class JabKitTest extends AbstractJabKitTest {

    @Test
    void auxImport(@TempDir Path tempDir) throws IOException, URISyntaxException {
        String fullBib = getClassResourceAsFullyQualifiedString("origin.bib");
        String auxFile = getClassResourceAsFullyQualifiedString("paper.aux");

        Path outputBib = tempDir.resolve("output.bib").toAbsolutePath();

        List<String> args = List.of("generate-bib-from-aux", "--aux", auxFile, "--input", fullBib, "--output", outputBib.toString());

        commandLine.execute(args.toArray(String[]::new));

        assertFileExists(outputBib);
    }

    @Test
    void checkConsistency() {
        Path testBib = getClassResourceAsPath("origin.bib");
        String testBibFile = testBib.toAbsolutePath().toString();

        // The input file is passed as a positional argument (see ADR-0057).
        List<String> args = List.of("check", "consistency", testBibFile, "--output-format", "txt");

        int executionResult = executeToLog(args.toArray(String[]::new));

        String output = getStandardOutput();
        assertTrue(output.contains("Checking consistency for entry type 1 of 1\n"), "Expected output to contain sentence: Checking consistency for entry type 1 of 1");
        assertTrue(output.contains("Consistency check completed"), "Expected output to contain sentence: Consistency check completed");
        assertEquals(0, executionResult);
    }

    @Test
    void checkConsistencyPorcelain() {
        Path testBib = getClassResourceAsPath("origin.bib");
        String testBibFile = testBib.toAbsolutePath().toString();

        // errorformat is the default output format; thus not provided here.
        // The legacy --input option is still accepted as an alias for the positional argument.
        List<String> args = List.of("check", "consistency", "--input", testBibFile, "--porcelain");

        int executionResult = executeToLog(args.toArray(String[]::new));

        String output = getStandardOutput();
        assertEquals("", output);
        assertEquals(0, executionResult);
    }

    @Test
    void checkConsistencyFailsWithoutInputFile() {
        int executionResult = executeToLog("check", "consistency");

        // picocli reports a usage error (exit code 2) when no input file is supplied.
        assertEquals(2, executionResult);
    }

    @Test
    void checkWithoutSubcommandRunsBothChecks() {
        Path testBib = getClassResourceAsPath("origin.bib");
        String testBibFile = testBib.toAbsolutePath().toString();

        // No subcommand: passing the file directly to "check" runs consistency and integrity.
        List<String> args = List.of("check", testBibFile, "--output-format", "txt");

        int executionResult = executeToLog(args.toArray(String[]::new));

        String output = getStandardOutput();
        assertTrue(output.contains("Checking consistency of"), "Expected the consistency check to run");
        assertTrue(output.contains("Checking integrity of"), "Expected the integrity check to run");
    }
}
