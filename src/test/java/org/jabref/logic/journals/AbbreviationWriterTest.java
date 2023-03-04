package org.jabref.logic.journals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
class AbbreviationWriterTest {

    @Test
    void shortestUniqueAbbreviationWrittenIfItDiffers(@TempDir Path tempDir) throws Exception {
        Abbreviation abbreviation = new Abbreviation("Full", "Abbr", "A");
        Path csvFile = tempDir.resolve("test.csv");
        AbbreviationWriter.writeOrCreate(
                csvFile,
                List.of(abbreviation));
        assertEquals(List.of("Full;Abbr;A"), Files.readAllLines(csvFile));
    }

    @Test
    void doNotWriteShortestUniqueAbbreviationWrittenIfItDiffers(@TempDir Path tempDir) throws Exception {
        Abbreviation abbreviation = new Abbreviation("Full", "Abbr");
        Path csvFile = tempDir.resolve("test.csv");
        AbbreviationWriter.writeOrCreate(
                csvFile,
                List.of(abbreviation));
        assertEquals(List.of("Full;Abbr"), Files.readAllLines(csvFile));
    }
}
