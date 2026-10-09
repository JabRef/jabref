package org.jabref.toolkit.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.toolkit.commands.AbstractJabKitTest;
import org.jabref.toolkit.exception.ExportServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ExportServiceTest extends AbstractJabKitTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void bibtexExportPreservesContextAndEntrySelection(int selectedEntryCount, @TempDir Path tempDir) throws Exception {
        Path source = getClassResourceAsPath("/org/jabref/toolkit/commands/strings-and-preamble.bib");
        BibDatabase database = ImportService.importBibTexFile(source, preferences, true).getDatabase();
        BibEntry selected = database.getEntries().getFirst();
        BibEntry excluded = ImportService.importBibTexFile(getClassResourceAsPath("origin.bib"), preferences, true)
                                         .getDatabase().getEntries().getFirst();
        database.insertEntry(excluded);
        List<BibEntry> entries = database.getEntries().subList(0, selectedEntryCount);
        Path output = tempDir.resolve("selected.bib");

        new ExportService(preferences, true).exportBibDatabaseContextToFile(
                new BibDatabaseContext(database), entries, output, "bibtex");

        BibDatabase actual = ImportService.importBibTexFile(output, preferences, true).getDatabase();
        assertEquals(entries, actual.getEntries());
        assertEquals(database.getPreamble(), actual.getPreamble());
        assertEquals("Journal of Reproducible Tests", actual.getStringByName("journalname").orElseThrow().getContent());
        assertEquals(List.of(selected, excluded), database.getEntries());
    }

    @BeforeEach
    void setup() {
        SelfContainedSaveOrder selfContainedSaveOrder = new SelfContainedSaveOrder(SaveOrder.OrderType.ORIGINAL, List.of());
        SelfContainedSaveConfiguration selfContainedSaveConfiguration = new SelfContainedSaveConfiguration(selfContainedSaveOrder, false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        when(preferences.getSelfContainedExportConfiguration()).thenReturn(selfContainedSaveConfiguration);
    }

    @ParameterizedTest
    @CsvSource({"bibtex", "html", "simplehtml", "tablerefs", "oocsv", "hayagrivayaml", "iso690rtf"})
    void differentOutputFormatsExportFile(String format, @TempDir Path tempDir) throws Exception {
        Path source = getClassResourceAsPath("origin.bib").toAbsolutePath();
        ParserResult parserResult = ImportService.importBibTexFile(source, preferences, true);
        Path output = tempDir.resolve("output." + format);

        new ExportService(preferences, false).exportParserResultToFile(parserResult, output, format);

        assertFileExists(output);
    }

    @Test
    void simpleOutputTest(@TempDir Path tempDir) throws Exception {
        Path source = getClassResourceAsPath("origin.bib").toAbsolutePath();
        ParserResult parserResult = ImportService.importBibTexFile(source, preferences, true);
        Path output = tempDir.resolve("output.bibtex");

        new ExportService(preferences, false).exportParserResultToFile(parserResult, output, "bibtex");

        assertTrue(Files.readString(output).contains("Darwin1888"));
    }

    @Test
    void wrongOutputFormatFails(@TempDir Path tempDir) throws Exception {
        Path source = getClassResourceAsPath("origin.bib").toAbsolutePath();
        ParserResult parserResult = ImportService.importBibTexFile(source, preferences, true);
        Path output = tempDir.resolve("output.bibtex");

        String invalidFormat = "Klingon";

        assertThrows(ExportServiceException.class,
                () -> new ExportService(preferences, false)
                        .exportParserResultToFile(parserResult, output, invalidFormat));
    }

    @Test
    void convertBibtexToTableRefsAsBib(@TempDir Path tempDir) throws Exception {
        Path source = getClassResourceAsPath("origin.bib").toAbsolutePath();
        ParserResult parserResult = ImportService.importBibTexFile(source, preferences, true);
        Path outputHtml = tempDir.resolve("output.html").toAbsolutePath();

        SaveOrder saveOrder = new SaveOrder(SaveOrder.OrderType.TABLE, List.of());
        ExportPreferences exportPreferences = new ExportPreferences(".html", tempDir, saveOrder, List.of());
        when(preferences.getExportPreferences()).thenReturn(exportPreferences);

        new ExportService(preferences, false).exportParserResultToFile(parserResult, outputHtml, "tablerefsabsbib");

        assertFileExists(outputHtml);
    }
}
