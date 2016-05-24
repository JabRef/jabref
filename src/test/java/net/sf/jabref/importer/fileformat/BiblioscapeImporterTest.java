package net.sf.jabref.importer.fileformat;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BiblioscapeImporterTest {

    private BiblioscapeImporter bsImporter;

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        bsImporter = new BiblioscapeImporter();
    }

    @Test
    public void testGetFormatName() {
        BiblioscapeImporter importer = new BiblioscapeImporter();
        Assert.assertEquals(importer.getFormatName(), "Biblioscape");
    }

    @Test
    public void testGetCLIID() {
        BiblioscapeImporter importer = new BiblioscapeImporter();
        Assert.assertEquals(importer.getId(), "biblioscape");
    }

    @Test
    public void testImportEntriesAbortion() throws Throwable {
        Path file = Paths.get(BiblioscapeImporter.class.getResource("BiblioscapeImporterTestCorrupt.txt").toURI());
        Assert.assertEquals(Collections.emptyList(), bsImporter.importDatabase(file, Charset.defaultCharset())
                .getDatabase().getEntries());
    }
}
