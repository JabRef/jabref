package org.jabref.toolkit.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.toolkit.exception.CliExceptionHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ConvertTest extends AbstractJabKitTest {

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
    void noOutputPrintsBibtexToStdout(@TempDir Path tempDir) throws IOException {
        SelfContainedSaveOrder saveOrder = new SelfContainedSaveOrder(SaveOrder.OrderType.ORIGINAL, List.of());
        when(preferences.getSelfContainedExportConfiguration())
                .thenReturn(new SelfContainedSaveConfiguration(saveOrder, false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false));

        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path newPath = tempDir.resolve("origin.bib");
        Files.copy(origin, newPath);

        commandLine.executeToLog("convert",
                "--input=" + newPath, "--input-format=bibtex");

        assertTrue(commandLine.getStandardOutput().contains("@Book{Darwin1888,"));
    }

    @Test
    void noOutputExportsRequestedOutputFormat(@TempDir Path tempDir) throws IOException {
        SelfContainedSaveOrder saveOrder = new SelfContainedSaveOrder(SaveOrder.OrderType.ORIGINAL, List.of());
        when(preferences.getSelfContainedExportConfiguration())
                .thenReturn(new SelfContainedSaveConfiguration(saveOrder, false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false));

        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path newPath = tempDir.resolve("origin.bib");
        Files.copy(origin, newPath);

        int exitCode = commandLine.executeToLog("convert",
                "--input=" + newPath,
                "--input-format=bibtex",
                "--output-format=html");

        assertEquals(0, exitCode);
        assertTrue(commandLine.getStandardOutput().contains("<html"));
        assertFalse(commandLine.getStandardOutput().contains("@Book"));
        assertTrue(commandLine.getErrorOutput().contains("Converting"));
    }

    @Test
    void noOutputWithUnknownOutputFormatFailsWithUsageError(@TempDir Path tempDir) throws IOException {
        commandLine.setExecutionExceptionHandler(
                new CliExceptionHandler(commandLine.getExecutionExceptionHandler()));

        Path origin = getClassResourceAsPath("origin.bib").toAbsolutePath();
        Path newPath = tempDir.resolve("origin.bib");
        Files.copy(origin, newPath);

        int exitCode = commandLine.executeToLog("convert",
                "--input=" + newPath,
                "--input-format=bibtex",
                "--output-format=unknownformat");

        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
        assertTrue(commandLine.getErrorOutput().contains("Unknown export format 'unknownformat'."));
        assertFalse(commandLine.getStandardOutput().contains("@Book"));
    }

    @Test
    void fieldFormattersAreAppliedDuringConversion(@TempDir Path tempDir) throws IOException {
        Path newPath = tempDir.resolve("origin.bib");
        String originBibtex = """
                @Article{test_energy,
                  title   = {my ﬁrst research},
                  pages   = {1-10},
                  month   = {January},
                  comment = {private note}
                }""";
        Files.writeString(newPath, originBibtex);

        Path outputPath = tempDir.resolve("output");

        commandLine.execute("convert",
                "--input=" + newPath,
                "--input-format=bibtex",
                "--output=" + outputPath,
                "--output-format=bibtex",
                "--field-formatters=pages[normalize_page_numbers],month[normalize_month],All-text-fields[replace_unicode_ligatures],comment[clear]");

        assertFileExists(outputPath);
        String outputContent = Files.readString(outputPath);

        assertTrue(outputContent.contains("1--10"));

        assertTrue(outputContent.contains("#jan#"));

        assertTrue(outputContent.contains("first"));

        assertFalse(outputContent.contains("private note"));
    }
}
