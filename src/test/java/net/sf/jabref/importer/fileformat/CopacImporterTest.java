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
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CopacImporterTest {

    @Before
    public void setUp() throws Exception {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {

        CopacImporter importer = new CopacImporter();
        try (InputStream stream1 = CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest1.txt"); InputStream stream2 = CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest2.txt"); InputStream stream3 = CopacImporterTest.class
                .getResourceAsStream("IsiImporterTest1.isi"); InputStream stream4 = CopacImporterTest.class
                .getResourceAsStream("IsiImporterTestInspec.isi"); InputStream stream5 = CopacImporterTest.class
                .getResourceAsStream("IsiImporterTestWOS.isi"); InputStream stream6 = CopacImporterTest.class
                .getResourceAsStream("IsiImporterTestMedline.isi")) {

            Assert.assertTrue(importer.isRecognizedFormat(stream1));

            Assert.assertTrue(importer.isRecognizedFormat(stream2));

            Assert.assertFalse(importer.isRecognizedFormat(stream3));

            Assert.assertFalse(importer.isRecognizedFormat(stream4));

            Assert.assertFalse(importer.isRecognizedFormat(stream5));

            Assert.assertFalse(importer.isRecognizedFormat(stream6));
        }
    }

    @Test
    public void testImportEntries() throws IOException {
        Globals.prefs.put("defaultEncoding", StandardCharsets.UTF_8.name());

        CopacImporter importer = new CopacImporter();

        try (InputStream stream = CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest1.txt")) {
            List<BibEntry> entries = importer.importEntries(stream, new OutputPrinterToNull());
            Assert.assertEquals(1, entries.size());
            BibEntry entry = entries.get(0);

            Assert.assertEquals("The SIS project : software reuse with a natural language approach", entry.getField("title"));
            Assert.assertEquals(
"Prechelt, Lutz and Universität Karlsruhe. Fakultät für Informatik",
                    entry.getField("author"));
            Assert.assertEquals("Interner Bericht ; Nr.2/92", entry.getField("series"));
            Assert.assertEquals("1992", entry.getField("year"));
            Assert.assertEquals("Karlsruhe :  Universitat Karlsruhe, Fakultat fur Informatik", entry.getField("publisher"));
            Assert.assertEquals(BibtexEntryTypes.BOOK, entry.getType());
        }
    }

    @Test
    public void testImportEntries2() throws IOException {
        CopacImporter importer = new CopacImporter();

        try (InputStream stream = CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest2.txt")) {
            List<BibEntry> entries = importer.importEntries(stream, new OutputPrinterToNull());
            Assert.assertEquals(2, entries.size());
            BibEntry one = entries.get(0);

            Assert.assertEquals("Computing and operational research at the London Hospital", one.getField("title"));

            BibEntry two = entries.get(1);

            Assert.assertEquals("Real time systems : management and design", two.getField("title"));
        }
    }
}
