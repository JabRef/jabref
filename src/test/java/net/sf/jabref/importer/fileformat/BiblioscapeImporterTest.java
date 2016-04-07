package net.sf.jabref.importer.fileformat;

import java.io.InputStream;
import java.util.Collections;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
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
        Assert.assertEquals(importer.getCLIId(), "biblioscape");
    }

    @Test
    public void testImportEntriesAbortion() throws Throwable {
        try (InputStream is = BiblioscapeImporter.class.getResourceAsStream("BiblioscapeImporterTestCorrupt.txt")) {
            Assert.assertEquals(Collections.emptyList(), bsImporter.importEntries(is, new OutputPrinterToNull()));
        }
    }
}
