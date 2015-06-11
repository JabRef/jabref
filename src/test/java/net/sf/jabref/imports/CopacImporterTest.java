package net.sf.jabref.imports;

import net.sf.jabref.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

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
        assertTrue(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest1.txt")));

        assertTrue(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest2.txt")));

        assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("IsiImporterTest1.isi")));

        assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("IsiImporterTestInspec.isi")));

        assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("IsiImporterTestWOS.isi")));

        assertFalse(importer.isRecognizedFormat(CopacImporterTest.class
                .getResourceAsStream("IsiImporterTestMedline.isi")));
    }

    @Test @Ignore
    public void testImportEntries() throws IOException {
        CopacImporter importer = new CopacImporter();

        List<BibtexEntry> entries = importer.importEntries(CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest1.txt"), new OutputPrinterToNull());
        assertEquals(1, entries.size());
        BibtexEntry entry = entries.get(0);

        assertEquals("The SIS project : software reuse with a natural language approach", entry.getField("title"));
        assertEquals(
                "Prechelt, Lutz and Universität Karlsruhe. Fakultät für Informatik",
                entry.getField("author"));
        assertEquals("Interner Bericht ; Nr.2/92", entry.getField("series"));
        assertEquals("1992", entry.getField("year"));
        assertEquals("Karlsruhe :  Universitat Karlsruhe, Fakultat fur Informatik", entry.getField("publisher"));
        assertEquals(BibtexEntryType.BOOK, entry.getType());
    }

    @Test
    public void testImportEntries2() throws IOException {
        CopacImporter importer = new CopacImporter();

        List<BibtexEntry> entries = importer.importEntries(CopacImporterTest.class
                .getResourceAsStream("CopacImporterTest2.txt"), new OutputPrinterToNull());
        assertEquals(2, entries.size());
        BibtexEntry one = entries.get(0);

        assertEquals("Computing and operational research at the London Hospital", one.getField("title"));

        BibtexEntry two = entries.get(1);

        assertEquals("Real time systems : management and design", two.getField("title"));
    }
}
