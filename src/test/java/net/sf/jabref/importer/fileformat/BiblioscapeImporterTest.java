package net.sf.jabref.importer.fileformat;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BiblioscapeImporterTest {

    private BiblioscapeImporter importer;


    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        importer = new BiblioscapeImporter();
    }

    @Test
    public void testGetFormatName() {
        Assert.assertEquals(importer.getFormatName(), "Biblioscape");
    }

    @Test
    public void testsGetExtensions() {
        Assert.assertEquals(Arrays.asList(".txt"), importer.getExtensions());
    }

    @Test
    public void testGetDescription() {
        Assert.assertEquals("Imports a Biblioscape Tag File.\n" +
                "Several Biblioscape field types are ignored. Others are only included in the BibTeX field \"comment\".", importer.getDescription());
    }

    @Test
    public void testGetCLIID() {
        Assert.assertEquals(importer.getId(), "biblioscape");
    }

    @Test
    public void testImportEntriesAbortion() throws Throwable {
        Path file = Paths.get(BiblioscapeImporter.class.getResource("BiblioscapeImporterTestCorrupt.txt").toURI());
        Assert.assertEquals(Collections.emptyList(),
                importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries());
    }
}
