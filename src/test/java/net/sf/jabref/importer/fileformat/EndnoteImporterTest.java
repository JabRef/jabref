package net.sf.jabref.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.Globals;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


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
        assertEquals("refer", importer.getId());
    }

    @Test
    public void testsGetExtensions() {
        EndnoteImporter importer = new EndnoteImporter();
        List<String> extensions = new ArrayList<>();
        extensions.add(".enw");

        assertEquals(extensions.get(0), importer.getExtensions().get(0));
    }

    @Test
    public void testGetDescription() {
        EndnoteImporter importer = new EndnoteImporter();
        assertEquals("Importer for the Refer/Endnote format." +
                " Modified to use article number for pages if pages are missing.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("Endnote.pattern.A.enw", "Endnote.pattern.E.enw", "Endnote.book.example.enw");

        for (String str : list) {
            Path file = Paths.get(EndnoteImporterTest.class.getResource(str).toURI());
            assertTrue(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("IEEEImport1.txt", "IsiImporterTest1.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi", "RisImporterTest1.ris",
                "Endnote.pattern.no_enw", "empty.pdf", "annotated.pdf");

        for (String str : list) {
            Path file = Paths.get(EndnoteImporterTest.class.getResource(str).toURI());
            assertFalse(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testImportEntries0() throws IOException, URISyntaxException {
        Path file = Paths.get(EndnoteImporterTest.class.getResource("Endnote.entries.enw").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

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

    @Test
    public void testImportEntries1() throws IOException {
        String s = "%O Artn\\\\s testO\n%A testA,\n%E testE0, testE1";
        List<BibEntry> bibEntries = importer.importDatabase(new BufferedReader(new StringReader(s))).getDatabase().getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be = bibEntries.get(0);
        assertEquals("misc", be.getType());
        assertEquals("testA", be.getField("author"));
        assertEquals("testE0, testE1", be.getField("editor"));
        assertEquals("testO", be.getField("pages"));
    }

    @Test
    public void testImportEntriesBookExample() throws IOException, URISyntaxException {
        Path file = Paths.get(EndnoteImporterTest.class.getResource("Endnote.book.example.enw").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, Charset.defaultCharset()).getDatabase().getEntries();

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
