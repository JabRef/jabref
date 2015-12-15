package net.sf.jabref.importer.fileformat;

import net.sf.jabref.*;

import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

/**
 * Test cases for the RISImporter
 *
 * @author $Author: coezbek $
 */
public class RISImporterTest {

    @Before
    public void setUp() throws Exception {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {

        RisImporter importer = new RisImporter();
        try (InputStream stream = RISImporterTest.class
                .getResourceAsStream("RisImporterTest1.ris")) {
            Assert.assertTrue(importer.isRecognizedFormat(stream));
        }
    }

    @Test
    public void testProcessSubSup() {

        HashMap<String, String> hm = new HashMap<>();
        hm.put("title", "/sub 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_3$", hm.get("title"));

        hm.put("title", "/sub   3   /");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_3$", hm.get("title"));

        hm.put("title", "/sub 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_{31}$", hm.get("title"));

        hm.put("abstract", "/sub 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_3$", hm.get("abstract"));

        hm.put("review", "/sub 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_{31}$", hm.get("review"));

        hm.put("title", "/sup 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^3$", hm.get("title"));

        hm.put("title", "/sup 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^{31}$", hm.get("title"));

        hm.put("abstract", "/sup 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^3$", hm.get("abstract"));

        hm.put("review", "/sup 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^{31}$", hm.get("review"));

        hm.put("title", "/sub $Hello/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_{\\$Hello}$", hm.get("title"));
    }

    @Test
    public void testImportEntries() throws IOException {
        RisImporter importer = new RisImporter();

        try (InputStream stream = RISImporterTest.class
                .getResourceAsStream("RisImporterTest1.ris")) {
            List<BibEntry> entries = importer.importEntries(stream, new OutputPrinterToNull());
            Assert.assertEquals(1, entries.size());
            BibEntry entry = entries.get(0);
            Assert.assertEquals("Editorial: Open Source and Empirical Software Engineering", entry
                    .getField("title"));
            Assert.assertEquals(
                    "Harrison, Warren",
                    entry.getField("author"));

            Assert.assertEquals(BibtexEntryTypes.ARTICLE, entry.getType());
            Assert.assertEquals("Empirical Software Engineering", entry.getField("journal"));
            Assert.assertEquals("2001", entry.getField("year"));
            Assert.assertEquals("6", entry.getField("volume"));
            Assert.assertEquals("3", entry.getField("number"));
            Assert.assertEquals("193--194", entry.getField("pages"));
            Assert.assertEquals("#sep#", entry.getField("month"));
        }
    }
}
