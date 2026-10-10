package org.jabref.toolkit.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ExportServiceTest extends AbstractJabKitTest {

    // [utest->req~jabkit.cli.convert-bibtex-context~1]
    @ParameterizedTest
    @CsvSource({"0, false", "1, false", "2, false", "0, true", "1, true", "2, true"})
    void bibtexExportPreservesContextAndEntrySelection(int selectedEntryCount, boolean shared, @TempDir Path tempDir) throws Exception {
        Path source = getClassResourceAsPath("/org/jabref/toolkit/commands/strings-and-preamble.bib");
        BibDatabase database = ImportService.importBibTexFile(source, preferences, true).getDatabase();
        BibEntry selected = database.getEntries().getFirst();
        BibEntry excluded = ImportService.importBibTexFile(getClassResourceAsPath("origin.bib"), preferences, true)
                                         .getDatabase().getEntries().getFirst();
        database.insertEntry(excluded);
        Optional<String> sharedDatabaseId = shared ? Optional.of("source-library") : Optional.empty();
        sharedDatabaseId.ifPresent(database::setSharedDatabaseID);
        List<BibEntry> entries = database.getEntries().subList(0, selectedEntryCount);
        Path output = tempDir.resolve("selected.bib");

        new ExportService(preferences, true).exportBibDatabaseContextToFile(
                new BibDatabaseContext(database), entries, output, "bibtex");

        BibDatabase actual = ImportService.importBibTexFile(output, preferences, true).getDatabase();
        assertEquals(entries, actual.getEntries());
        assertEquals(database.getPreamble(), actual.getPreamble());
        assertEquals("Journal of Reproducible Tests", actual.getStringByName("journalname").orElseThrow().getContent());
        assertEquals(List.of(selected, excluded), database.getEntries());
        Optional<String> expectedSharedDatabaseId = selectedEntryCount == database.getEntryCount() ? sharedDatabaseId : Optional.empty();
        assertEquals(expectedSharedDatabaseId, actual.getSharedDatabaseID());
        assertEquals(sharedDatabaseId, database.getSharedDatabaseID());
    }

    // [utest->req~jabkit.cli.convert-bibtex-context~1]
    @Test
    void saveDatabaseContextPreservesSharedDatabaseId(@TempDir Path tempDir) throws Exception {
        Path source = getClassResourceAsPath("/org/jabref/toolkit/commands/strings-and-preamble.bib");
        BibDatabaseContext context = ImportService.importBibTexFile(source, preferences, true).getDatabaseContext();
        context.getDatabase().setSharedDatabaseID("source-library");
        Path output = tempDir.resolve("saved.bib");

        new ExportService(preferences, true).saveDatabaseContext(context, output);

        BibDatabase actual = ImportService.importBibTexFile(output, preferences, true).getDatabase();
        assertEquals(Optional.of("source-library"), actual.getSharedDatabaseID());
        assertEquals(context.getDatabase().getEntries(), actual.getEntries());
        assertEquals(context.getDatabase().getPreamble(), actual.getPreamble());
        assertEquals("Journal of Reproducible Tests", actual.getStringByName("journalname").orElseThrow().getContent());
        assertEquals(Optional.of("source-library"), context.getDatabase().getSharedDatabaseID());
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
