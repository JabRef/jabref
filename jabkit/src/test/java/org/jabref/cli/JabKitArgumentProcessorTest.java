package org.jabref.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import javafx.collections.FXCollections;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.support.BibEntryAssert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JabKitArgumentProcessorTest {

    private final CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
    private final ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final ExportPreferences exportPreferences = mock(ExportPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

    private CommandLine commandLine;

    @BeforeEach()
    void setup() {
        when(importerPreferences.getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());
        when(exportPreferences.getCustomExporters()).thenReturn(FXCollections.emptyObservableList());

        when(preferences.getExportPreferences()).thenReturn(exportPreferences);
        when(preferences.getImporterPreferences()).thenReturn(importerPreferences);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        when(preferences.getSearchPreferences()).thenReturn(new SearchPreferences(
                SearchDisplayMode.FILTER,
                EnumSet.noneOf(SearchFlags.class),
                false,
                false,
                0,
                0,
                0));

        JabKitArgumentProcessor argumentProcessor = new JabKitArgumentProcessor(preferences, entryTypesManager);
        commandLine = new CommandLine(argumentProcessor);
    }

    @Test
    void auxImport(@TempDir Path tempDir) throws URISyntaxException {
        String fullBib = Path.of(JabKitJabKitArgumentProcessorTest.class.getResource("origin.bib").toURI()).toAbsolutePath().toString();
        String auxFile = Path.of(JabKitJabKitArgumentProcessorTest.class.getResource("paper.aux").toURI()).toAbsolutePath().toString();

        Path outputBib = tempDir.resolve("output.bib").toAbsolutePath();

        List<String> args = List.of("generate-bib-from-aux", "--aux", auxFile, "--input", fullBib, "--output", outputBib.toString());

        commandLine.execute(args.toArray(String[]::new));

        assertTrue(Files.exists(outputBib));
    }

    @Test
    void search(@TempDir Path tempDir) throws URISyntaxException, IOException {
        Path originBib = Path.of(Objects.requireNonNull(JabKitArgumentProcessorTest.class.getResource("origin.bib")).toURI());
        String originBibFile = originBib.toAbsolutePath().toString();

        Path expectedBib = Path.of(
                Objects.requireNonNull(JabKitArgumentProcessorTest.class.getResource("ArgumentProcessorTestExportMatches.bib"))
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

    @Test
    void convertBibtexToTableRefsAsBib(@TempDir Path tempDir) throws URISyntaxException {
        Path originBib = Path.of(Objects.requireNonNull(JabKitArgumentProcessorTest.class.getResource("origin.bib")).toURI());
        String originBibFile = originBib.toAbsolutePath().toString();

        Path outputHtml = tempDir.resolve("output.html").toAbsolutePath();
        String outputHtmlFile = outputHtml.toAbsolutePath().toString();

        when(importerPreferences.getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());

        SaveOrder saveOrder = new SaveOrder(SaveOrder.OrderType.TABLE, List.of());
        ExportPreferences exportPreferences = new ExportPreferences(".html", tempDir, saveOrder, List.of());
        when(preferences.getExportPreferences()).thenReturn(exportPreferences);

        SelfContainedSaveOrder selfContainedSaveOrder = new SelfContainedSaveOrder(SaveOrder.OrderType.ORIGINAL, List.of());
        SelfContainedSaveConfiguration selfContainedSaveConfiguration = new SelfContainedSaveConfiguration(selfContainedSaveOrder, false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        when(preferences.getSelfContainedExportConfiguration()).thenReturn(selfContainedSaveConfiguration);

        List<String> args = List.of("convert", "--input", originBibFile, "--input-format", "bibtex", "--output", outputHtmlFile, "--output-format", "tablerefsabsbib");

        commandLine.execute(args.toArray(String[]::new));

        assertTrue(Files.exists(outputHtml));
    }

    @Test
    void checkConsistency() throws URISyntaxException {
        Path testBib = Path.of(Objects.requireNonNull(JabKitArgumentProcessorTest.class.getResource("origin.bib")).toURI());
        String testBibFile = testBib.toAbsolutePath().toString();

        List<String> args = List.of("check-consistency", "--input", testBibFile, "--output-format", "txt");

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true));

        int executionResult = commandLine.execute(args.toArray(String[]::new));

        String output = outContent.toString().replace("\r\n", "\n");
        assertTrue(output.contains("Checking consistency for entry type 1 of 1\n"), "Expected output to contain sentence: Checking consistency for entry type 1 of 1");
        assertTrue(output.contains("Consistency check completed"), "Expected output to contain sentence: Consistency check completed");
        assertEquals(0, executionResult);

        System.setOut(System.out);
    }

    @Test
    void checkConsistencyPorcelain() throws URISyntaxException {
        Path testBib = Path.of(Objects.requireNonNull(JabKitArgumentProcessorTest.class.getResource("origin.bib")).toURI());
        String testBibFile = testBib.toAbsolutePath().toString();

        // "txt" is the default output format; thus not provided here
        List<String> args = List.of("check-consistency", "--input", testBibFile, "--porcelain");

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        int executionResult = commandLine.execute(args.toArray(String[]::new));

        String output = outContent.toString();
        assertEquals("", output);
        assertEquals(0, executionResult);

        System.setOut(System.out);
    }
}
