package org.jabref.toolkit.commands;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

public class ConvertTest extends AbstractJabKitTest {
    @ParameterizedTest
    @CsvSource({"bibtex", "html",
            "simplehtml", "tablerefs",
            "oocsv", "hayagrivayaml",
            "iso690rtf"
    })
    void differentOutputFormatTest(String format, @TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path newPath = tempDir.resolve("origin.bib");
        Files.copy(origin, newPath);
        Path outputPath = tempDir.resolve("output");

        // assertEquals(commandLine.execute("convert", "--input=" + newPath, "--input-format=bibtex", "--output-format=" + format, "--output=" + outputPath), 0);
        commandLine.execute("convert",
                "--input=" + newPath, "--input-format=bibtex",
                "--output-format=" + format,
                "--output=" + outputPath);

        assertFileExists(outputPath);
    }

    @Test
    void simpleOutputTest(@TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path newPath = tempDir.resolve("origin.bib");
        Files.copy(origin, newPath);
        Path outputPath = tempDir.resolve("output");

        // assertEquals(commandLine.execute("convert", "--input=" + newPath, "--input-format=bibtex", "--output-format=" + format, "--output=" + outputPath), 0);
        commandLine.execute("convert",
                "--input=" + newPath, "--input-format=bibtex",
                "--output-format=bibtex",
                "--output=" + outputPath);

        assertFileExists(outputPath);
        assertTrue(Files.readString(outputPath).contains("Darwin1888"));
    }

    @Test
    void wrongOutputFormatFails(@TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path newPath = tempDir.resolve("origin.bib");
        Files.copy(origin, newPath);
        Path outputPath = tempDir.resolve("output");

        // assertEquals(commandLine.execute("convert", "--input=" + newPath, "--input-format=bibtex", "--output-format=" + format, "--output=" + outputPath), 0);
        commandLine.execute("convert",
                "--input=" + newPath, "--input-format=bibtex",
                "--output-format=ffasdfasd",
                "--output=" + outputPath);

        assertFileDoesntExist(outputPath);
    }

    @Test
    void noOutputGeneratesNothing(@TempDir Path tempDir) throws IOException {
        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path newPath = tempDir.resolve("origin.bib");
        Files.copy(origin, newPath);

        // assertEquals(commandLine.execute("convert", "--input=" + newPath, "--input-format=bibtex", "--output-format=" + format, "--output=" + outputPath), 0);
        commandLine.execute("convert",
                "--input=" + newPath, "--input-format=bibtex",
                "--output-format=bibtex");

        assertEquals(1, Files.list(tempDir).collect(Collectors.toSet()).size());
    }

    @Test
    void convertBibtexToTableRefsAsBib(@TempDir Path tempDir) throws URISyntaxException {
        Path originBib = getClassResourceAsPath("origin.bib");
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

        assertFileExists(outputHtml);
    }
}
