package net.sf.jabref.importer.fileformat;

import net.sf.jabref.*;

import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class CopacImporterTest {

    @Before
    public void setUp() throws Exception {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
    }

    @Test
    public void testGetFormatName(){
        CopacImporter importer = new CopacImporter();
        Assert.assertEquals("Copac",importer.getFormatName());
    }

    @Test
    public void testGetCLIId(){
        CopacImporter importer = new CopacImporter();
        Assert.assertEquals("cpc",importer.getCLIId());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {

        CopacImporter importer = new CopacImporter();
        Assert.assertTrue(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest1.txt")));

        Assert.assertTrue(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest2.txt")));

        Assert.assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("IsiImporterTest1.isi")));

        Assert.assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("IsiImporterTestInspec.isi")));

        Assert.assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("IsiImporterTestWOS.isi")));

        Assert.assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("IsiImporterTestMedline.isi")));
    }

    @Test
    public void testImportEntries() throws IOException {
        Globals.prefs.put("defaultEncoding", "UTF8");

        CopacImporter importer = new CopacImporter();

        List<BibtexEntry> entries = importer.importEntries(CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest1.txt"), new OutputPrinterToNull());
        Assert.assertEquals(1, entries.size());
        BibtexEntry entry = entries.get(0);

        Assert.assertEquals("The SIS project : software reuse with a natural language approach", entry.getField("title"));
        Assert.assertEquals(
                "Prechelt, Lutz and Universität Karlsruhe. Fakultät für Informatik",
                entry.getField("author"));
        Assert.assertEquals("Interner Bericht ; Nr.2/92", entry.getField("series"));
        Assert.assertEquals("1992", entry.getField("year"));
        Assert.assertEquals("Karlsruhe :  Universitat Karlsruhe, Fakultat fur Informatik", entry.getField("publisher"));
        Assert.assertEquals(BibtexEntryTypes.BOOK, entry.getType());
    }

    @Test
    public void testImportEntries2() throws IOException {
        CopacImporter importer = new CopacImporter();

        List<BibtexEntry> entries = importer.importEntries(CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest2.txt"), new OutputPrinterToNull());
        Assert.assertEquals(2, entries.size());
        BibtexEntry one = entries.get(0);

        Assert.assertEquals("Computing and operational research at the London Hospital", one.getField("title"));

        BibtexEntry two = entries.get(1);

        Assert.assertEquals("Real time systems : management and design", two.getField("title"));
    }

    @Test
    public void testSetOrAppend1() throws IOException{
        CopacImporter importer = new CopacImporter();
        List<BibtexEntry> entries = importer.importEntries(CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest1.txt"), new OutputPrinterToNull());

        BibtexEntry one = entries.get(0);

        


    }
}
