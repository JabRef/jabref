package org.jabref.logic.journals;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbbreviationParserTest {

    private Path csvFile;
    private final AbbreviationParser parser = new AbbreviationParser();

    private final Abbreviation abbreviation = new Abbreviation("Long Name", "L.N.", "L.N.");

    @BeforeEach
    void setup(@TempDir Path tempDir) {
        csvFile = tempDir.resolve("test.csv");
    }

    @ParameterizedTest(name = "readJournalListFromFile with {0} delimiter")
    @MethodSource("provideDelimiterTestCases")
    void readJournalListFromFileWithDelimiters(String delimitername, String csvContent) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
            writer.write(csvContent);
        }
        parser.readJournalListFromFile(csvFile);
        assertEquals(Set.of(abbreviation), parser.getAbbreviations());
    }

    private static Stream<Arguments> provideDelimiterTestCases() {
        return Stream.of(
                // Test semicolon delimiter
                Arguments.of("semicolon", "Long Name;L.N.;L.N."),

                // Test comma delimiter
                Arguments.of("comma", "Long Name,L.N.,L.N.")
        );
    }
}
