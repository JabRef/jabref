package net.sf.jabref.imports;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Nosh&Dan
 * @version 09.11.2008 | 19:41:40
 */
public class ImportDataTest {

    public static final File FILE_IN_DATABASE = new File("src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder/pdfInDatabase.pdf");
    public static final File FILE_NOT_IN_DATABASE = new File("src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder/pdfNotInDatabase.pdf");
    public static final File EXISTING_FOLDER = new File("src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder");
    public static final File NOT_EXISTING_FOLDER = new File("notexistingfolder");
    public static final File NOT_EXISTING_PDF = new File("src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder/null.pdf");
    public static final File UNLINKED_FILES_TEST_BIB = new File("src/test/resources/net/sf/jabref/util/unlinkedFilesTestBib.bib");

    /**
     * Tests the testing environment.
     */
    @Test
    public void testTestingEnvironment() {
        assertTrue(EXISTING_FOLDER.exists());
        assertTrue(EXISTING_FOLDER.isDirectory());

        assertTrue(FILE_IN_DATABASE.exists());
        assertTrue(FILE_IN_DATABASE.isFile());

        assertTrue(FILE_NOT_IN_DATABASE.exists());
        assertTrue(FILE_NOT_IN_DATABASE.isFile());
    }

    @Test
    public void testOpenNotExistingDirectory() {
        assertFalse(NOT_EXISTING_FOLDER.exists());
        assertFalse(NOT_EXISTING_PDF.exists());
    }

}
