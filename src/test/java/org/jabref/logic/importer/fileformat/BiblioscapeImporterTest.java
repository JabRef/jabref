package org.jabref.logic.importer.fileformat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.jabref.logic.util.FileExtensions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BiblioscapeImporterTest {

    private BiblioscapeImporter importer;


    @Before
    public void setUp() throws Exception {
        importer = new BiblioscapeImporter();
    }

    @Test
    public void testGetFormatName() {
        Assert.assertEquals("Biblioscape", importer.getName());
    }

    @Test
    public void testsGetExtensions() {
        Assert.assertEquals(FileExtensions.BILBIOSCAPE, importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        Assert.assertEquals("Imports a Biblioscape Tag File.\n" +
                "Several Biblioscape field types are ignored. Others are only included in the BibTeX field \"comment\".", importer.getDescription());
    }

    @Test
    public void testGetCLIID() {
        Assert.assertEquals("biblioscape", importer.getId());
    }

    @Test
    public void testImportEntriesAbortion() throws Throwable {
        Path file = Paths.get(BiblioscapeImporter.class.getResource("BiblioscapeImporterTestCorrupt.txt").toURI());
        Assert.assertEquals(Collections.emptyList(),
                importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries());
    }
}
