package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.util.FileExtensions;
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
        importer = new EndnoteImporter(ImportFormatPreferences.fromPreferences(JabRefPreferences.getInstance()));
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

        assertEquals(FileExtensions.ENDNOTE, importer.getExtensions());

    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the Refer/Endnote format."
                + " Modified to use article number for pages if pages are missing.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("Endnote.pattern.A.enw", "Endnote.pattern.E.enw", "Endnote.book.example.enw");

        for (String str : list) {
            Path file = Paths.get(EndnoteImporterTest.class.getResource(str).toURI());
            assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
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
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(5, bibEntries.size());

        BibEntry be0 = bibEntries.get(0);
        assertEquals("misc", be0.getType());
        assertEquals(Optional.of("testA0 and testA1"), be0.getFieldOptional("author"));
        assertEquals(Optional.of("testE0 and testE1"), be0.getFieldOptional("editor"));
        assertEquals(Optional.of("testT"), be0.getFieldOptional("title"));

        BibEntry be1 = bibEntries.get(1);
        assertEquals("misc", be1.getType());
        assertEquals(Optional.of("testC"), be1.getFieldOptional("address"));
        assertEquals(Optional.of("testB2"), be1.getFieldOptional("booktitle"));
        assertEquals(Optional.of("test8"), be1.getFieldOptional("date"));
        assertEquals(Optional.of("test7"), be1.getFieldOptional("edition"));
        assertEquals(Optional.of("testJ"), be1.getFieldOptional("journal"));
        assertEquals(Optional.of("testD"), be1.getFieldOptional("year"));

        BibEntry be2 = bibEntries.get(2);
        assertEquals("article", be2.getType());
        assertEquals(Optional.of("testB0"), be2.getFieldOptional("journal"));

        BibEntry be3 = bibEntries.get(3);
        assertEquals("book", be3.getType());
        assertEquals(Optional.of("testI0"), be3.getFieldOptional("publisher"));
        assertEquals(Optional.of("testB1"), be3.getFieldOptional("series"));

        BibEntry be4 = bibEntries.get(4);
        assertEquals("mastersthesis", be4.getType());
        assertEquals(Optional.of("testX"), be4.getFieldOptional("abstract"));
        assertEquals(Optional.of("testF"), be4.getFieldOptional("bibtexkey"));
        assertEquals(Optional.of("testR"), be4.getFieldOptional("doi"));
        assertEquals(Optional.of("testK"), be4.getFieldOptional("keywords"));
        assertEquals(Optional.of("testO1"), be4.getFieldOptional("note"));
        assertEquals(Optional.of("testN"), be4.getFieldOptional("number"));
        assertEquals(Optional.of("testP"), be4.getFieldOptional("pages"));
        assertEquals(Optional.of("testI1"), be4.getFieldOptional("school"));
        assertEquals(Optional.of("testU"), be4.getFieldOptional("url"));
        assertEquals(Optional.of("testV"), be4.getFieldOptional("volume"));
    }

    @Test
    public void testImportEntries1() throws IOException {
        String s = "%O Artn\\\\s testO\n%A testA,\n%E testE0, testE1";
        List<BibEntry> bibEntries = importer.importDatabase(new BufferedReader(new StringReader(s))).getDatabase()
                .getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be = bibEntries.get(0);
        assertEquals("misc", be.getType());
        assertEquals(Optional.of("testA"), be.getFieldOptional("author"));
        assertEquals(Optional.of("testE0, testE1"), be.getFieldOptional("editor"));
        assertEquals(Optional.of("testO"), be.getFieldOptional("pages"));
    }

    @Test
    public void testImportEntriesBookExample() throws IOException, URISyntaxException {
        Path file = Paths.get(EndnoteImporterTest.class.getResource("Endnote.book.example.enw").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be = bibEntries.get(0);
        assertEquals("book", be.getType());
        assertEquals(Optional.of("Heidelberg"), be.getFieldOptional("address"));
        assertEquals(Optional.of("Preißel, René and Stachmann, Bjørn"), be.getFieldOptional("author"));
        assertEquals(Optional.of("3., aktualisierte und erweiterte Auflage"), be.getFieldOptional("edition"));
        assertEquals(Optional.of("Versionsverwaltung"), be.getFieldOptional("keywords"));
        assertEquals(Optional.of("XX, 327"), be.getFieldOptional("pages"));
        assertEquals(Optional.of("dpunkt.verlag"), be.getFieldOptional("publisher"));
        assertEquals(Optional.of("Git : dezentrale Versionsverwaltung im Team : Grundlagen und Workflows"),
                be.getFieldOptional("title"));
        assertEquals(Optional.of("http://d-nb.info/107601965X"), be.getFieldOptional("url"));
        assertEquals(Optional.of("2016"), be.getFieldOptional("year"));
    }
}
