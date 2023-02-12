package org.jabref.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import javafx.collections.FXCollections;

import org.jabref.cli.ArgumentProcessor.Mode;
import org.jabref.logic.bibtex.BibEntryAssert;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArgumentProcessorTest {

    private ArgumentProcessor processor;
    private BibtexImporter bibtexImporter;
    private final PreferencesService preferencesService = mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS);
    private final SavePreferences savePreferences = mock(SavePreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

    @BeforeEach()
    void setup() {
        when(savePreferences.getSaveOrder()).thenReturn(SaveOrderConfig.getDefaultSaveOrder());
        when(preferencesService.getSavePreferences()).thenReturn(savePreferences);
        when(importerPreferences.getCustomImportList()).thenReturn(FXCollections.emptyObservableSet());
        when(preferencesService.getSearchPreferences()).thenReturn(
                new SearchPreferences(null, EnumSet.noneOf(SearchRules.SearchFlags.class), false)
        );

        bibtexImporter = new BibtexImporter(importFormatPreferences, new DummyFileUpdateMonitor());
    }

    @Test
    void testAuxImport(@TempDir Path tempDir) throws Exception {

        String auxFile = Path.of(AuxCommandLineTest.class.getResource("paper.aux").toURI()).toAbsolutePath().toString();
        String originBib = Path.of(AuxCommandLineTest.class.getResource("origin.bib").toURI()).toAbsolutePath().toString();

        Path outputBib = tempDir.resolve("output.bisb").toAbsolutePath();
        String outputBibFile = outputBib.toAbsolutePath().toString();

        List<String> args = List.of("--nogui", "--debug", "--aux", auxFile + "," + outputBibFile, originBib);

        processor = new ArgumentProcessor(args.toArray(String[]::new), Mode.INITIAL_START, preferencesService);

        assertTrue(Files.exists(outputBib));
    }

    @Test
    void testExportMatches(@TempDir Path tempDir) throws Exception {
        Path originBib = Path.of(Objects.requireNonNull(ArgumentProcessorTest.class.getResource("origin.bib")).toURI());
        String originBibFile = originBib.toAbsolutePath().toString();

        Path expectedBib = Path.of(
                Objects.requireNonNull(ArgumentProcessorTest.class.getResource("ArgumentProcessorTestExportMatches.bib"))
                       .toURI()
        );
        List<BibEntry> expectedEntries = bibtexImporter.importDatabase(expectedBib).getDatabase().getEntries();

        Path outputBib = tempDir.resolve("output.bib").toAbsolutePath();
        String outputBibFile = outputBib.toAbsolutePath().toString();

        List<String> args = List.of("-n", "--debug", "--exportMatches", "Author=Einstein," + outputBibFile, originBibFile);

        processor = new ArgumentProcessor(args.toArray(String[]::new), Mode.INITIAL_START, preferencesService);

        assertTrue(Files.exists(outputBib));
        BibEntryAssert.assertEquals(expectedEntries, outputBib, bibtexImporter);
    }
}
