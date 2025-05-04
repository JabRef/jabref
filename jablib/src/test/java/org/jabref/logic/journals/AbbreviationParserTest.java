package org.jabref.logic.journals;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbbreviationParserTest {

    private Path csvFile;
    private final AbbreviationParser parser = new AbbreviationParser();

    private final Abbreviation abbreviation = new Abbreviation("Long Name", "L.N.", "L.N.");

    @BeforeEach
    void setup(@TempDir Path tempDir) {
        csvFile = tempDir.resolve("test.csv");
    }

    @Test
    void readingFileFromCSVWithSemicolon() throws IOException {
        // String name, String abbreviation, String shortestUniqueAbbreviation
        String testAbbrev = "Long Name;L.N.;L.N.";
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write(testAbbrev);
        }
        parser.readJournalListFromFile(csvFile);
        assertEquals(Set.of(abbreviation), parser.getAbbreviations());
    }

    @Test
    void readingFileFromCSVWithComma() throws IOException {
        String testAbbrev = "Long Name,L.N.,L.N.";
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write(testAbbrev);
        }
        parser.readJournalListFromFile(csvFile);
        assertEquals(Set.of(abbreviation), parser.getAbbreviations());
    }
}
