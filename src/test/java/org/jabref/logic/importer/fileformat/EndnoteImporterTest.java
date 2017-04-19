package org.jabref.logic.importer.fileformat;

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

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.FileExtensions;
import org.jabref.model.entry.BibEntry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class EndnoteImporterTest {

    private EndnoteImporter importer;


    @Before
    public void setUp() {
        importer = new EndnoteImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    public void testGetFormatName() {
        assertEquals("Refer/Endnote", importer.getName());
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
        assertEquals(Optional.of("testA0 and testA1"), be0.getField("author"));
        assertEquals(Optional.of("testE0 and testE1"), be0.getField("editor"));
        assertEquals(Optional.of("testT"), be0.getField("title"));

        BibEntry be1 = bibEntries.get(1);
        assertEquals("misc", be1.getType());
        assertEquals(Optional.of("testC"), be1.getField("address"));
        assertEquals(Optional.of("testB2"), be1.getField("booktitle"));
        assertEquals(Optional.of("test8"), be1.getField("date"));
        assertEquals(Optional.of("test7"), be1.getField("edition"));
        assertEquals(Optional.of("testJ"), be1.getField("journal"));
        assertEquals(Optional.of("testD"), be1.getField("year"));

        BibEntry be2 = bibEntries.get(2);
        assertEquals("article", be2.getType());
        assertEquals(Optional.of("testB0"), be2.getField("journal"));

        BibEntry be3 = bibEntries.get(3);
        assertEquals("book", be3.getType());
        assertEquals(Optional.of("testI0"), be3.getField("publisher"));
        assertEquals(Optional.of("testB1"), be3.getField("series"));

        BibEntry be4 = bibEntries.get(4);
        assertEquals("mastersthesis", be4.getType());
        assertEquals(Optional.of("testX"), be4.getField("abstract"));
        assertEquals(Optional.of("testF"), be4.getField("bibtexkey"));
        assertEquals(Optional.of("testR"), be4.getField("doi"));
        assertEquals(Optional.of("testK"), be4.getField("keywords"));
        assertEquals(Optional.of("testO1"), be4.getField("note"));
        assertEquals(Optional.of("testN"), be4.getField("number"));
        assertEquals(Optional.of("testP"), be4.getField("pages"));
        assertEquals(Optional.of("testI1"), be4.getField("school"));
        assertEquals(Optional.of("testU"), be4.getField("url"));
        assertEquals(Optional.of("testV"), be4.getField("volume"));
    }

    @Test
    public void testImportEntries1() throws IOException {
        String s = "%O Artn\\\\s testO\n%A testA,\n%E testE0, testE1";
        List<BibEntry> bibEntries = importer.importDatabase(new BufferedReader(new StringReader(s))).getDatabase()
                .getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be = bibEntries.get(0);
        assertEquals("misc", be.getType());
        assertEquals(Optional.of("testA"), be.getField("author"));
        assertEquals(Optional.of("testE0, testE1"), be.getField("editor"));
        assertEquals(Optional.of("testO"), be.getField("pages"));
    }

    @Test
    public void testImportEntriesBookExample() throws IOException, URISyntaxException {
        Path file = Paths.get(EndnoteImporterTest.class.getResource("Endnote.book.example.enw").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be = bibEntries.get(0);
        assertEquals("book", be.getType());
        assertEquals(Optional.of("Heidelberg"), be.getField("address"));
        assertEquals(Optional.of("Preißel, René and Stachmann, Bjørn"), be.getField("author"));
        assertEquals(Optional.of("3., aktualisierte und erweiterte Auflage"), be.getField("edition"));
        assertEquals(Optional.of("Versionsverwaltung"), be.getField("keywords"));
        assertEquals(Optional.of("XX, 327"), be.getField("pages"));
        assertEquals(Optional.of("dpunkt.verlag"), be.getField("publisher"));
        assertEquals(Optional.of("Git : dezentrale Versionsverwaltung im Team : Grundlagen und Workflows"),
                be.getField("title"));
        assertEquals(Optional.of("http://d-nb.info/107601965X"), be.getField("url"));
        assertEquals(Optional.of("2016"), be.getField("year"));
    }
}
