package net.sf.jabref.importer.fileformat;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinterToNull;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class EndnoteImporterTest {

    private EndnoteImporter importer;

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        importer = new EndnoteImporter();
    }

    @Test
    public void testGetFormatName() {
        assertEquals("Refer/Endnote", importer.getFormatName());
    }

    @Test
    public void testGetCLIId() {
        assertEquals("refer", importer.getCLIId());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {
        List<String> list = Arrays.asList("Endnote.pattern.A.enw", "Endnote.pattern.E.enw", "Endnote.book.example.enw");

        for (String str : list) {
            try (InputStream is = EndnoteImporterTest.class.getResourceAsStream(str)) {
                assertTrue(importer.isRecognizedFormat(is));
            }
        }
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException {
        List<String> list = Arrays.asList("IEEEImport1.txt", "IsiImporterTest1.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi", "RisImporterTest1.ris",
                "Endnote.pattern.no_enw", "empty.pdf", "annotated.pdf");

        for (String str : list) {
            try (InputStream is = EndnoteImporterTest.class.getResourceAsStream(str)) {
                assertFalse(importer.isRecognizedFormat(is));
            }
        }
    }

    @Test
    public void testImportEntries0() throws IOException {
        try (InputStream is = EndnoteImporterTest.class.getResourceAsStream("Endnote.entries.enw")) {
            List<BibEntry> bibEntries = importer.importEntries(is, new OutputPrinterToNull());

            assertEquals(5, bibEntries.size());

            BibEntry be0 = bibEntries.get(0);
            assertEquals("misc", be0.getType());
            assertEquals("testA0 and testA1", be0.getField("author"));
            assertEquals("testE0 and testE1", be0.getField("editor"));
            assertEquals("testT", be0.getField("title"));

            BibEntry be1 = bibEntries.get(1);
            assertEquals("misc", be1.getType());
            assertEquals("testC", be1.getField("address"));
            assertEquals("testB2", be1.getField("booktitle"));
            assertEquals("test8", be1.getField("date"));
            assertEquals("test7", be1.getField("edition"));
            assertEquals("testJ", be1.getField("journal"));
            assertEquals("testD", be1.getField("year"));

            BibEntry be2 = bibEntries.get(2);
            assertEquals("article", be2.getType());
            assertEquals("testB0", be2.getField("journal"));

            BibEntry be3 = bibEntries.get(3);
            assertEquals("book", be3.getType());
            assertEquals("testI0", be3.getField("publisher"));
            assertEquals("testB1", be3.getField("series"));

            BibEntry be4 = bibEntries.get(4);
            assertEquals("mastersthesis", be4.getType());
            assertEquals("testX", be4.getField("abstract"));
            assertEquals("testF", be4.getField("bibtexkey"));
            assertEquals("testR", be4.getField("doi"));
            assertEquals("testK", be4.getField("keywords"));
            assertEquals("testO1", be4.getField("note"));
            assertEquals("testN", be4.getField("number"));
            assertEquals("testP", be4.getField("pages"));
            assertEquals("testI1", be4.getField("school"));
            assertEquals("testU", be4.getField("url"));
            assertEquals("testV", be4.getField("volume"));
        }
    }

    @Test
    public void testImportEntries1() throws IOException {
        String s = "%O Artn\\\\s testO\n%A testA,\n%E testE0, testE1";
        InputStream is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        List<BibEntry> bibEntries = importer.importEntries(is, new OutputPrinterToNull());

        assertEquals(1, bibEntries.size());

        BibEntry be = bibEntries.get(0);
        assertEquals("misc", be.getType());
        assertEquals("testA", be.getField("author"));
        assertEquals("testE0, testE1", be.getField("editor"));
        assertEquals("testO", be.getField("pages"));
    }

    @Test
    public void testImportEntriesBookExample() throws IOException {
        try (InputStream is = EndnoteImporterTest.class.getResourceAsStream("Endnote.book.example.enw")) {
            List<BibEntry> bibEntries = importer.importEntries(is, new OutputPrinterToNull());

            assertEquals(1, bibEntries.size());

            BibEntry be = bibEntries.get(0);
            assertEquals("book", be.getType());
            assertEquals("Heidelberg", be.getField("address"));
            assertEquals("Preißel, René and Stachmann, Bjørn", be.getField("author"));
            assertEquals("3., aktualisierte und erweiterte Auflage", be.getField("edition"));
            assertEquals("Versionsverwaltung", be.getField("keywords"));
            assertEquals("XX, 327", be.getField("pages"));
            assertEquals("dpunkt.verlag", be.getField("publisher"));
            assertEquals("Git : dezentrale Versionsverwaltung im Team : Grundlagen und Workflows", be.getField("title"));
            assertEquals("http://d-nb.info/107601965X", be.getField("url"));
            assertEquals("2016", be.getField("year"));
        }
    }
}
