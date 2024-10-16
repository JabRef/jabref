package org.jabref.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import javafx.collections.FXCollections;

import org.jabref.cli.ArgumentProcessor.Mode;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArgumentProcessorTest {

    private final GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
    private final ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

    @BeforeEach()
    void setup() {
        when(importerPreferences.getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());

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
                preferences,
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

        List<String> args = List.of("-n", "--debug", "--exportMatches", "author=Einstein," + outputBibFile, originBibFile);

        ArgumentProcessor processor = new ArgumentProcessor(
                args.toArray(String[]::new),
                Mode.INITIAL_START,
                preferences,
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
        when(preferences.getExportPreferences()).thenReturn(exportPreferences);

        SelfContainedSaveOrder selfContainedSaveOrder = new SelfContainedSaveOrder(SaveOrder.OrderType.ORIGINAL, List.of());
        SelfContainedSaveConfiguration selfContainedSaveConfiguration = new SelfContainedSaveConfiguration(selfContainedSaveOrder, false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        when(preferences.getSelfContainedExportConfiguration()).thenReturn(selfContainedSaveConfiguration);

        List<String> args = List.of("-n", "-i", originBibFile + ",bibtex", "-o", outputHtmlFile + ",tablerefsabsbib");

        ArgumentProcessor processor = new ArgumentProcessor(
                args.toArray(String[]::new),
                Mode.INITIAL_START,
                preferences,
                mock(FileUpdateMonitor.class),
                entryTypesManager);
        processor.processArguments();

        assertTrue(Files.exists(outputHtml));
    }
}
