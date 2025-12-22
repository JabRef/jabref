package org.jabref.toolkit.commands;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.support.BibEntryAssert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// TODO: These tests do not work on linux with "org.gradle.workers.max" greater than 1.
@Execution(ExecutionMode.SAME_THREAD)
// Embedded postgres is started per test class and causes conflicts for file "libicuuc.so"
public class SearchTest extends AbstractJabKitTest {
    @Test
    @ResourceLock("embeddedPostgres")
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
    @ResourceLock("embeddedPostgres")
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
    @ResourceLock("embeddedPostgres")
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

    @Test
    @ResourceLock("embeddedPostgres")
    void search(@TempDir Path tempDir) throws URISyntaxException, IOException {
        Path originBib = getClassResourceAsPath("origin.bib");
        String originBibFile = originBib.toAbsolutePath().toString();

        Path expectedBib = Path.of(
                Objects.requireNonNull(JabKitTest.class.getResource("ArgumentProcessorTestExportMatches.bib"))
                       .toURI()
        );

        BibtexImporter bibtexImporter = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor());
        List<BibEntry> expectedEntries = bibtexImporter.importDatabase(expectedBib).getDatabase().getEntries();

        Path outputBib = tempDir.resolve("output.bib").toAbsolutePath();

        List<String> args = List.of("search", "--debug", "--query", "author=Einstein", "--input", originBibFile, "--output", outputBib.toString());

        commandLine.execute(args.toArray(String[]::new));

        assertTrue(Files.exists(outputBib));
        BibEntryAssert.assertEquals(expectedEntries, outputBib, bibtexImporter);
    }
}
