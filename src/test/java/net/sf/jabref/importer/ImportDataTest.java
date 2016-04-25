package net.sf.jabref.importer;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Nosh&Dan
 * @version 09.11.2008 | 19:41:40
 */
public class ImportDataTest {

    public static final File FILE_IN_DATABASE = Paths
            .get("src/test/resources/net/sf/jabref/importer/unlinkedFilesTestFolder/pdfInDatabase.pdf").toFile();
    public static final File FILE_NOT_IN_DATABASE = Paths
            .get("src/test/resources/net/sf/jabref/importer/unlinkedFilesTestFolder/pdfNotInDatabase.pdf").toFile();
    public static final File EXISTING_FOLDER = Paths
            .get("src/test/resources/net/sf/jabref/importer/unlinkedFilesTestFolder").toFile();
    public static final File NOT_EXISTING_FOLDER = Paths.get("notexistingfolder").toFile();
    public static final File NOT_EXISTING_PDF = Paths
            .get("src/test/resources/net/sf/jabref/importer/unlinkedFilesTestFolder/null.pdf").toFile();
    public static final File UNLINKED_FILES_TEST_BIB = Paths
            .get("src/test/resources/net/sf/jabref/util/unlinkedFilesTestBib.bib").toFile();


    /**
     * Tests the testing environment.
     */
    @Test
    public void testTestingEnvironment() {

        Assert.assertTrue(ImportDataTest.EXISTING_FOLDER.exists());
        Assert.assertTrue(ImportDataTest.EXISTING_FOLDER.isDirectory());

        Assert.assertTrue(ImportDataTest.FILE_IN_DATABASE.exists());
        Assert.assertTrue(ImportDataTest.FILE_IN_DATABASE.isFile());

        Assert.assertTrue(ImportDataTest.FILE_NOT_IN_DATABASE.exists());
        Assert.assertTrue(ImportDataTest.FILE_NOT_IN_DATABASE.isFile());
    }

    @Test
    public void testOpenNotExistingDirectory() {
        Assert.assertFalse(ImportDataTest.NOT_EXISTING_FOLDER.exists());
        Assert.assertFalse(ImportDataTest.NOT_EXISTING_PDF.exists());
    }

}
