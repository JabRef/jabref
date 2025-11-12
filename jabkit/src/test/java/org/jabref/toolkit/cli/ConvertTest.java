package org.jabref.toolkit.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(outputPath.toFile().exists());
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

        assertTrue(outputPath.toFile().exists());
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

        assertFalse(outputPath.toFile().exists());
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
}
