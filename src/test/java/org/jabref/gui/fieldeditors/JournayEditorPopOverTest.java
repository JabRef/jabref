package org.jabref.gui.fieldeditors;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JournalEditorPopOverTest {

    private Path tempFolder;

    @BeforeEach
    void setUp(@TempDir Path tempFolder) throws Exception {
        this.tempFolder = tempFolder; // Set up the temp folder
        // 1) Create Journal Object

        // Test entry
        // 6, 58530, National vital statistics reports : from the Centers for Disease Control and Prevention,
        // National Center for Health Statistics, National Vital Statistics System,	book series,	15518922, 15518930
        // 29,810,	Q1,	95,	14,	30,	379, 1059, 30, 41,61, 27,07, United States, Northern America
        // Public Health Services, US Dept of Health and Human Services	1998-2020	Life-span and Life-course Studies (Q1)
    }

    @Test
    void testValidISSN() {
        // Check if valid ISSN returns something
        Path file = tempFolder.resolve("<Filename>.json"); // Load in the file
        // Call method with valid ISSN
//        String expectedJournalName = journalObject.getJournalNameByISSN(file, 15518922);
        String expectedJournalName = "Hello";
        String trueJournalName = "National vital statistics reports : from the Centers for Disease Control and Prevention, " +
                "National Center for Health Statistics, National Vital Statistics System";

        assertEquals(trueJournalName, expectedJournalName);
    }

    @Test
    void testInvalidISSN() {
        // Check if invalid ISSN returns something
        Path file = tempFolder.resolve("<Filename>.json"); // Load in the file
        // Call method with valid ISSN
//        String expectedJournalName = journalObject.getJournalNameByISSN(file, 15518922); // Should return null or something
        String expectedJournalName = null;
        String trueJournalName = null; // Or empty String

        assertEquals(expectedJournalName, trueJournalName);
    }

    @Test
    void testReadInput() {
        Path expectedFile = tempFolder.resolve("test.json"); // Load in the file
        Optional<String> file = journalObject.getFileByISSN(); // .... Load in the file

        // If file is not found
        assertTrue(file.isPresent()); // File is present
    }
}
