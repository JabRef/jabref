package org.jabref.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import javafx.collections.FXCollections;

import org.jabref.cli.ArgumentProcessor.Mode;
import org.jabref.gui.Globals;
import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.ExportPreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArgumentProcessorTest {

    private final PreferencesService preferencesService = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
    private final BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
    private final ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

    @BeforeEach()
    void setup() {
        when(importerPreferences.getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());

        when(preferencesService.getImporterPreferences()).thenReturn(importerPreferences);
        when(preferencesService.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        when(preferencesService.getSearchPreferences()).thenReturn(
                new SearchPreferences(null, EnumSet.noneOf(SearchRules.SearchFlags.class), false)
        );
    }

    @Test
    void auxImport(@TempDir Path tempDir) throws Exception {
        String auxFile = Path.of(AuxCommandLineTest.class.getResource("paper.aux").toURI()).toAbsolutePath().toString();
        String originBib = Path.of(AuxCommandLineTest.class.getResource("origin.bib").toURI()).toAbsolutePath().toString();

        Path outputBib = tempDir.resolve("output.bisb").toAbsolutePath();
        String outputBibFile = outputBib.toAbsolutePath().toString();

        List<String> args = List.of("--nogui", "--debug", "--aux", auxFile + "," + outputBibFile, originBib);

        ArgumentProcessor processor = new ArgumentProcessor(
                args.toArray(String[]::new),
                Mode.INITIAL_START,
                preferencesService,
                mock(FileUpdateMonitor.class),
                entryTypesManager);
        processor.processArguments();

        assertTrue(Files.exists(outputBib));
    }

    @Test
    void exportMatches(@TempDir Path tempDir) throws Exception {
        Path originBib = Path.of(Objects.requireNonNull(ArgumentProcessorTest.class.getResource("origin.bib")).toURI());
        String originBibFile = originBib.toAbsolutePath().toString();

        Path expectedBib = Path.of(
                Objects.requireNonNull(ArgumentProcessorTest.class.getResource("ArgumentProcessorTestExportMatches.bib"))
                       .toURI()
        );

        BibtexImporter bibtexImporter = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor());
        List<BibEntry> expectedEntries = bibtexImporter.importDatabase(expectedBib).getDatabase().getEntries();

        Path outputBib = tempDir.resolve("output.bib").toAbsolutePath();
        String outputBibFile = outputBib.toAbsolutePath().toString();

        List<String> args = List.of("-n", "--debug", "--exportMatches", "Author=Einstein," + outputBibFile, originBibFile);

        ArgumentProcessor processor = new ArgumentProcessor(
                args.toArray(String[]::new),
                Mode.INITIAL_START,
                preferencesService,
                mock(FileUpdateMonitor.class),
                entryTypesManager);
        processor.processArguments();

        assertTrue(Files.exists(outputBib));
        BibEntryAssert.assertEquals(expectedEntries, outputBib, bibtexImporter);
    }

    @Test
    void convertBibtexToTablerefsabsbib(@TempDir Path tempDir) throws Exception {
        Path originBib = Path.of(Objects.requireNonNull(ArgumentProcessorTest.class.getResource("origin.bib")).toURI());
        String originBibFile = originBib.toAbsolutePath().toString();

        Path outputHtml = tempDir.resolve("output.html").toAbsolutePath();
        String outputHtmlFile = outputHtml.toAbsolutePath().toString();

        when(importerPreferences.getCustomImporters()) .thenReturn(FXCollections.emptyObservableSet());

        SaveOrder saveOrder = new SaveOrder(SaveOrder.OrderType.TABLE, List.of());
        ExportPreferences exportPreferences = new ExportPreferences(".html", tempDir, saveOrder, List.of());
        when(preferencesService.getExportPreferences()).thenReturn(exportPreferences);

        SelfContainedSaveOrder selfContainedSaveOrder = new SelfContainedSaveOrder(SaveOrder.OrderType.ORIGINAL, List.of());
        SelfContainedSaveConfiguration selfContainedSaveConfiguration = new SelfContainedSaveConfiguration(selfContainedSaveOrder, false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        when(preferencesService.getSelfContainedExportConfiguration()).thenReturn(selfContainedSaveConfiguration);

        List<String> args = List.of("-n", "-i", originBibFile + ",bibtex", "-o", outputHtmlFile + ",tablerefsabsbib");

        ArgumentProcessor processor = new ArgumentProcessor(
                args.toArray(String[]::new),
                Mode.INITIAL_START,
                preferencesService,
                mock(FileUpdateMonitor.class),
                entryTypesManager);
        processor.processArguments();

        assertTrue(Files.exists(outputHtml));
    }

    @Test
    void printVersion() throws Exception {
        // This test will attempt to verify that when using the CLI
        // with the flag --version that the appropriate version information
        // is printed.
        // The test checks that a line including "JabRef" followed
        // by a version number is printed and if not then the test fails.

        // Redirect stdout to capture CLI output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream stdout = System.out;
        System.setOut(new PrintStream(outputStream));

        List<String> args = List.of("-n", "--version");

        // Simulate processArguments with --version
        ArgumentProcessor processor = new ArgumentProcessor(
                args.toArray(String[]::new),
                Mode.INITIAL_START,
                preferencesService,
                mock(FileUpdateMonitor.class),
                entryTypesManager);
        processor.processArguments();
        String output = outputStream.toString();

        // Check that output contains version information
        List<String> version = List.of(output.split(" "));
        String versionNr = String.valueOf(Globals.BUILD_INFO.version);
        assertEquals("JabRef", version.get(0));
        assertTrue(version.get(1).contains(versionNr));

        // Reset stdout
        System.setOut(stdout);
    }

    @Test
    void fetchEntriesFromWeb() throws Exception {
        // This test checks if using the --fetch flag for
        // the CLI works correctly. The test will attempt
        // searching a query given a fetcher and verify that no
        // error occurred when fetching. Whether the fetch found 20
        // results or 0 the test will still pass, it will also pass
        // if an invalid fetcher was given as long as the program
        // properly reports it. However, if a network error occurred during
        // a fetch then the test will fail.

        // Redirect stout to capture cli output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream stdout = System.out;
        System.setOut(new PrintStream(outputStream));

        // Setup parameters for fetching
        String found = "Found \\d+ results\\.";
        String notFound = "No results found\\.";
        String fetcher = "ACM Portal";
        String query = "AI";
        String invalidFetcher = "Could not find fetcher '" + fetcher + "'";
        boolean fetchSucceeded = false;
        List<String> args = List.of("-n", "--fetch=" + fetcher + ":" + query);

        // Simulate processArguments with --fetch
        ArgumentProcessor processor = new ArgumentProcessor(
                args.toArray(String[]::new),
                Mode.INITIAL_START,
                preferencesService,
                mock(FileUpdateMonitor.class),
                entryTypesManager);
        processor.processArguments();
        String output = outputStream.toString();

        List<String> result = List.of(output.split("\r"));
        // Check that proper result are displayed to the user
        for (String outputRow : result) {
            if (outputRow.matches(found) || outputRow.matches(notFound) || outputRow.matches(invalidFetcher)) {
                fetchSucceeded = true;
                break;
            }
        }

        assertTrue(fetchSucceeded);

        // Reset stdout
        System.setOut(stdout);
    }

    @Test
    void tooManyWriteFlags() throws Exception {
        // This test checks that if too many -write flags
        // are given to the CLI that the program correctly
        // reports the error. Specifically in this test, the flags
        // -writeMetadatatoPdf and -writeXMPtoPdf are used.

        // Redirect stderr to capture cli output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream stderr = System.err;
        System.setErr(new PrintStream(outputStream));

        List<String> args = List.of("-n", "-writeMetadatatoPdf all", "-writeXMPtoPdf all");
        String expectedOutput = "Give only one of [writeXMPtoPdf, embeddBibfileInPdf, writeMetadatatoPdf";

        // Simulate processArguments with -writeMetadatatoPdf all -writeXMPtoPdf all
        ArgumentProcessor processor = new ArgumentProcessor(
                args.toArray(String[]::new),
                Mode.INITIAL_START,
                preferencesService,
                mock(FileUpdateMonitor.class),
                entryTypesManager);
        processor.processArguments();
        String output = outputStream.toString();

        assertTrue(output.contains(expectedOutput));

        // Reset stderr
        System.setOut(stderr);
    }
}
