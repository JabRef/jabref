package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JournalAbbreviationMvGeneratorTest {

    @TempDir
    Path tempDir;

    private Path csvFile;
    private Path mvFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a sample CSV file with two entries.
        csvFile = tempDir.resolve("testJournal.csv");
        String csvContent = "\"Test Journal\",\"T. J.\"\n" +
                "\"Another Journal\",\"A. J.\"";
        Files.writeString(csvFile, csvContent);

        // The expected MV file has the same base name with extension .mv.
        mvFile = tempDir.resolve("testJournal.mv");
    }

    @Test
    void convertCsvToMvCreatesMvFileAndLoadsCorrectly() throws IOException, InterruptedException {
        // Convert CSV to MV
        JournalAbbreviationMvGenerator.convertCsvToMv(csvFile, mvFile);

        // Verify the MV file is created
        assertTrue(Files.exists(mvFile));

        // Load abbreviations from the MV file
        Collection<Abbreviation> abbreviations = JournalAbbreviationMvGenerator.loadAbbreviationsFromMv(mvFile);
        // Expecting 2 abbreviations as in the CSV file
        assertEquals(2, abbreviations.size());

        // Check that the abbreviations match expected values
        boolean foundTestJournal = abbreviations.stream()
                                                .anyMatch(abbr -> "Test Journal".equalsIgnoreCase(abbr.getName()) &&
                                                        "T. J.".equals(abbr.getAbbreviation()));
        boolean foundAnotherJournal = abbreviations.stream()
                                                   .anyMatch(abbr -> "Another Journal".equalsIgnoreCase(abbr.getName()) &&
                                                           "A. J.".equals(abbr.getAbbreviation()));
        assertTrue(foundTestJournal);
        assertTrue(foundAnotherJournal);
    }

    @Test
    void convertAllCsvToMvIgnoresSpecifiedFiles(@TempDir Path testDir) throws IOException {
        // Create an ignored CSV file.
        Path ignoredCsv = testDir.resolve("journal_abbreviations_entrez.csv");
        Files.writeString(ignoredCsv, "\"Ignored Journal\",\"I. J.\"");

        // Create a valid CSV file.
        Path validCsv = testDir.resolve("validJournal.csv");
        Files.writeString(validCsv, "\"Valid Journal\",\"V. J.\"");

        // Run convertAllCsvToMv on the test directory.
        JournalAbbreviationMvGenerator.convertAllCsvToMv(testDir);

        // The ignored CSV file should not produce an MV file.
        Path ignoredMv = testDir.resolve("journal_abbreviations_entrez.mv");
        assertFalse(Files.exists(ignoredMv));

        // The valid CSV file should produce an MV file.
        Path validMv = testDir.resolve("validJournal.mv");
        assertTrue(Files.exists(validMv));
    }
}
