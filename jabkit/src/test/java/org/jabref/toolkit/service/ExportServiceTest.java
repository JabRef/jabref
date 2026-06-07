package org.jabref.toolkit.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.toolkit.commands.AbstractJabKitTest;
import org.jabref.toolkit.exception.ExportServiceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

class ExportServiceTest extends AbstractJabKitTest {

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
        try {
            Path source = getClassResourceAsPath("origin.bib").toAbsolutePath();
            ParserResult parserResult = ImportService.importBibTexFile(source, preferences, true);
            Path output = tempDir.resolve("output.bibtex");

            String invalidFormat = "Klingon";
            new ExportService(preferences, false).exportParserResultToFile(parserResult, output, invalidFormat);

            fail("An ExportServiceException should have been thrown");
        } catch (ExportServiceException e) {
            assertTrue(e.getMessage().contains("format"));
        }
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
