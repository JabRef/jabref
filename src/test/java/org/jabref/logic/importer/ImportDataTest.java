package org.jabref.logic.importer;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportDataTest {

    public static final Path FILE_IN_DATABASE = Path.of("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/pdfInDatabase.pdf");
    public static final Path FILE_NOT_IN_DATABASE = Path.of("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/pdfNotInDatabase.pdf");
    public static final Path EXISTING_FOLDER = Path.of("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder");
    public static final Path NOT_EXISTING_FOLDER = Path.of("notexistingfolder");
    public static final Path NOT_EXISTING_PDF = Path.of("src/test/resources/org/jabref/logic/importer/unlinkedFilesTestFolder/null.pdf");
    public static final Path UNLINKED_FILES_TEST_BIB = Path.of("src/test/resources/org/jabref/util/unlinkedFilesTestBib.bib");

    /**
     * Tests the testing environment.
     */
    @Test
    void testTestingEnvironment() {

        assertTrue(Files.exists(ImportDataTest.EXISTING_FOLDER));
        assertTrue(Files.isDirectory(ImportDataTest.EXISTING_FOLDER));

        assertTrue(Files.exists(ImportDataTest.FILE_IN_DATABASE));
        assertTrue(Files.isRegularFile(ImportDataTest.FILE_IN_DATABASE));

        assertTrue(Files.exists(ImportDataTest.FILE_NOT_IN_DATABASE));
        assertTrue(Files.isRegularFile(ImportDataTest.FILE_NOT_IN_DATABASE));

        assertFalse(Files.exists(ImportDataTest.NOT_EXISTING_FOLDER));
        assertFalse(Files.exists(ImportDataTest.NOT_EXISTING_PDF));
    }
}
