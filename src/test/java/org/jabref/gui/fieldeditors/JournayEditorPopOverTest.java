package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.jabref.logic.journals.JournalStatistics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalEditorPopOverTest {

    private Path tempFolder;
    private final String testEntry = "1;28773;CA - A Cancer Journal for Clinicians;journal;15424863, 00079235;88,192;Q1;156;36;129;2924;22644;89;255,73;81,22;United States;Northern America;Wiley-Blackwell;1950-2020;Hematology (Q1), Oncology (Q1)";
    private JournalStatistics jsReader;
    // Create object that loads in data from file

    /**
     * Each test should begin with assigning a temp folder to find the generated files
     * from the download. Then we have to create our object that loads in the file
     * and stores it temporarily.
     * @param tempFolder
     * @throws Exception
     */
    @BeforeEach
    void setUp(@TempDir Path tempFolder) throws Exception {
        this.tempFolder = tempFolder; // Set up the temp folder
        // Instantiate object that is used to read in data from file
    }

    /**
     * Given a valid ISSN this test should be able to find the file and return the correct
     * title for the journal corresponding to the valid ISSN.
     */
    @Test
    void testValidISSN() throws IOException {
        Path file = tempFolder.resolve("test.csv"); // Load in the file
        Files.writeString(file,
                testEntry, StandardOpenOption.CREATE); // Write the test entry to temp file

        int ISSN = 15424863;

        jsReader = new JournalStatistics(file);
        // Call method with valid ISSN with the object created during setup
        boolean found = jsReader.getTitleByISSN(ISSN); // This variable should hold a method call to finding the correct journal given a valid ISSN
        String trueJournalName = "CA - A Cancer Journal for Clinicians"; // true title of the found journal

        assertTrue(found);
    }

    /**
     * Given an invalid ISSN this test should be able to find the file and
     * return an empty title for the given ISSN because the ISSN is non-existent.
     */
    @Test
    void testInvalidISSN() throws IOException {
        Path file = tempFolder.resolve("test.csv"); // Load in the file
        Files.writeString(file,
                testEntry, StandardOpenOption.CREATE); // Write test entry to temp file

        int ISSN = 12300000;

        jsReader = new JournalStatistics(file);
        boolean found = jsReader.getTitleByISSN(ISSN);
        // Call method with invalid ISSN with the object created during setup
        String expectedJournalName = ""; // This variable is suppose to hold the title of the journal for the invalid ISSN
        String trueJournalName = "Nature Reviews Genetics"; // Or empty String

        assertFalse(found);
    }

    /**
     * This test regards testing whether the Python script or something similar is
     * able to download data and store it in a file located in a temp folder.
     */
    @Test
    void testReadInput() throws IOException {
        Path expectedFile = tempFolder.resolve("test.csv"); // Load in the file
        Files.writeString(expectedFile,
                testEntry, StandardOpenOption.CREATE);

        Optional<String> file = null; // This is where we would store our found file

        // If file is not found
        assertTrue(file.isPresent()); // File is present
    }
}
