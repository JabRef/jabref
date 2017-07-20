package net.sf.jabref.logic.importer.fileformat;

import net.sf.jabref.logic.util.FileExtensions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CsvImporterTest {
    private CsvImporter csvImporter;

    @Before
    public void SetUp() {
        csvImporter = new CsvImporter();
    }

    @After
    public void tearDown() {
        csvImporter = null;
    }

    @Test
    public void CsvExtentionFile() {
        assertEquals(FileExtensions.CSV, csvImporter.getExtensions());
    }
}
