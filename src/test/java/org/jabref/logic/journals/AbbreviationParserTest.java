package org.jabref.logic.journals;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbbreviationParserTest {

    private Path csvFile;
    private final AbbreviationParser parser = new AbbreviationParser();

    private final Abbreviation abbreviation = new Abbreviation("Long Name", "L.N.", "L.N.");

    @BeforeEach
    void setup(@TempDir Path tempDir) {
        csvFile = tempDir.resolve("test.csv");
    }

    @Test
    void testReadingFileFromCSVWithSemicolon() throws Exception {
        // String name, String abbreviation, String shortestUniqueAbbreviation
        String testAbbrev = "Long Name;L.N.;L.N.";
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write(testAbbrev);
        }
        parser.readJournalListFromFile(csvFile);
        assertEquals(Set.of(abbreviation), parser.getAbbreviations());
    }

    @Test
    void testReadingFileFromCSVWithComma() throws Exception {
        String testAbbrev = "Long Name,L.N.,L.N.";
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write(testAbbrev);
        }
        parser.readJournalListFromFile(csvFile);
        assertEquals(Set.of(abbreviation), parser.getAbbreviations());
    }
}
