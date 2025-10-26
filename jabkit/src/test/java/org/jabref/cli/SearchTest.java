package org.jabref.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SearchTest extends AbstractJabKitTest {
    @Test
    void foundSingleEntry(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("output");
        Path origin = getClassResourceAsPath("origin.bib");

        commandLine.execute("search",
                "--input=" + origin,
                "--query=Einstein",
                "--output-format=bibtex",
                "--output=" + output);

        assertTrue(Files.readString(output).contains("Einstein"));
        assertTrue(Files.readString(output).contains("Relativity"));
        assertFalse(Files.readString(output).contains("Newton"));
    }

    @Test
    void foundMultipleEntries(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("output");
        Path origin = getClassResourceAsPath("origin.bib");

        commandLine.execute("search",
                "--input=" + origin,
                "--query=19",
                "--output-format=bibtex",
                "--output=" + output);

        assertTrue(Files.readString(output).contains("Einstein"));
        assertTrue(Files.readString(output).contains("Relativity"));
        assertTrue(Files.readString(output).contains("Newton"));
        assertFalse(Files.readString(output).contains("Murray"));
    }

    @Test
    void foundNone(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("output");
        Path origin = getClassResourceAsPath("origin.bib");

        commandLine.execute("search",
                "--input=" + origin,
                "--query=blah",
                "--output-format=bibtex",
                "--output=" + output);

        assertFalse(output.toFile().exists());
    }
}
