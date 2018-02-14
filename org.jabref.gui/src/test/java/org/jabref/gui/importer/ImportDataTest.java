package org.jabref.gui.importer;

import java.io.File;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportDataTest {

    public static final File FILE_IN_DATABASE = Paths
            .get("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/pdfInDatabase.pdf").toFile();
    public static final File FILE_NOT_IN_DATABASE = Paths
            .get("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/pdfNotInDatabase.pdf")
            .toFile();
    public static final File EXISTING_FOLDER = Paths
            .get("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder").toFile();
    public static final File NOT_EXISTING_FOLDER = Paths.get("notexistingfolder").toFile();
    public static final File NOT_EXISTING_PDF = Paths
            .get("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/null.pdf").toFile();

    /**
     * Tests the testing environment.
     */
    @Test
    public void testTestingEnvironment() {

        assertTrue(ImportDataTest.EXISTING_FOLDER.exists());
        assertTrue(ImportDataTest.EXISTING_FOLDER.isDirectory());

        assertTrue(ImportDataTest.FILE_IN_DATABASE.exists());
        assertTrue(ImportDataTest.FILE_IN_DATABASE.isFile());

        assertTrue(ImportDataTest.FILE_NOT_IN_DATABASE.exists());
        assertTrue(ImportDataTest.FILE_NOT_IN_DATABASE.isFile());
    }

    @Test
    public void testOpenNotExistingDirectory() {
        assertFalse(ImportDataTest.NOT_EXISTING_FOLDER.exists());
        assertFalse(ImportDataTest.NOT_EXISTING_PDF.exists());
    }

}
