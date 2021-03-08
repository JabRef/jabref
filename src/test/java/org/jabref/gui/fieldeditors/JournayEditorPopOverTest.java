package org.jabref.gui.fieldeditors;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalEditorPopOverTest {

    private Path tempFolder;
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
    void testValidISSN() {
        Path file = tempFolder.resolve("<Filename>.json"); // Load in the file

        // Call method with valid ISSN with the object created during setup
        String expectedJournalName = ""; // This variable should hold a method call to finding the correct journal given a valid ISSN
        String trueJournalName = "National vital statistics reports : from the Centers for Disease Control and Prevention, " +
                "National Center for Health Statistics, National Vital Statistics System"; // true title of the found journal

        assertEquals(trueJournalName, expectedJournalName);
    }

    /**
     * Given an invalid ISSN this test should be able to find the file and
     * return an empty title for the given ISSN because the ISSN is non-existent.
     */
    @Test
    void testInvalidISSN() {
        Path file = tempFolder.resolve("<Filename>.json"); // Load in the file

        // Call method with invalid ISSN with the object created during setup
        String expectedJournalName = null; // This variable is suppose to hold the title of the journal for the invalid ISSN
        String trueJournalName = null; // Or empty String

        assertEquals(expectedJournalName, trueJournalName);
    }

    /**
     * This test regards testing whether the Python script or something similar is
     * able to download data and store it in a file located in a temp folder.
     */
    @Test
    void testReadInput() {
        Path expectedFile = tempFolder.resolve("test.json"); // Load in the file
        //  Use the object to call a method that loads in the file
        Optional<String> file = null; // This is where we would store our found file

        // If file is not found
        assertTrue(file.isPresent()); // File is present
    }
}
