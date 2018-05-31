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
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class EndnoteImporterTest {

    private EndnoteImporter importer;

    @BeforeEach
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
        assertEquals(FileType.ENDNOTE, importer.getFileType());
    }

    @Test
    public void testGetDescription() {
        assertEquals("Importer for the Refer/Endnote format."
                + " Modified to use article number for pages if pages are missing.", importer.getDescription());
    }

    @Test
    public void testIsRecognizedFormat() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("Endnote.pattern.A.enw", "Endnote.pattern.E.enw", "Endnote.book.example.enw");

        for (String string : list) {
            Path file = Paths.get(EndnoteImporterTest.class.getResource(string).toURI());
            assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("IEEEImport1.txt", "IsiImporterTest1.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi", "RisImporterTest1.ris",
                "Endnote.pattern.no_enw", "empty.pdf", "annotated.pdf");

        for (String string : list) {
            Path file = Paths.get(EndnoteImporterTest.class.getResource(string).toURI());
            assertFalse(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testImportEntries0() throws IOException, URISyntaxException {
        Path file = Paths.get(EndnoteImporterTest.class.getResource("Endnote.entries.enw").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(5, bibEntries.size());

        BibEntry first = bibEntries.get(0);
        assertEquals("misc", first.getType());
        assertEquals(Optional.of("testA0 and testA1"), first.getField("author"));
        assertEquals(Optional.of("testE0 and testE1"), first.getField("editor"));
        assertEquals(Optional.of("testT"), first.getField("title"));

        BibEntry second = bibEntries.get(1);
        assertEquals("misc", second.getType());
        assertEquals(Optional.of("testC"), second.getField("address"));
        assertEquals(Optional.of("testB2"), second.getField("booktitle"));
        assertEquals(Optional.of("test8"), second.getField("date"));
        assertEquals(Optional.of("test7"), second.getField("edition"));
        assertEquals(Optional.of("testJ"), second.getField("journal"));
        assertEquals(Optional.of("testD"), second.getField("year"));

        BibEntry third = bibEntries.get(2);
        assertEquals("article", third.getType());
        assertEquals(Optional.of("testB0"), third.getField("journal"));

        BibEntry fourth = bibEntries.get(3);
        assertEquals("book", fourth.getType());
        assertEquals(Optional.of("testI0"), fourth.getField("publisher"));
        assertEquals(Optional.of("testB1"), fourth.getField("series"));

        BibEntry fifth = bibEntries.get(4);
        assertEquals("mastersthesis", fifth.getType());
        assertEquals(Optional.of("testX"), fifth.getField("abstract"));
        assertEquals(Optional.of("testF"), fifth.getField("bibtexkey"));
        assertEquals(Optional.of("testR"), fifth.getField("doi"));
        assertEquals(Optional.of("testK"), fifth.getField("keywords"));
        assertEquals(Optional.of("testO1"), fifth.getField("note"));
        assertEquals(Optional.of("testN"), fifth.getField("number"));
        assertEquals(Optional.of("testP"), fifth.getField("pages"));
        assertEquals(Optional.of("testI1"), fifth.getField("school"));
        assertEquals(Optional.of("testU"), fifth.getField("url"));
        assertEquals(Optional.of("testV"), fifth.getField("volume"));
    }

    @Test
    public void testImportEntries1() throws IOException {
        String medlineString = "%O Artn\\\\s testO\n%A testA,\n%E testE0, testE1";
        List<BibEntry> bibEntries = importer.importDatabase(new BufferedReader(new StringReader(medlineString))).getDatabase()
                .getEntries();

        BibEntry entry = bibEntries.get(0);

        assertEquals(1, bibEntries.size());
        assertEquals("misc", entry.getType());
        assertEquals(Optional.of("testA"), entry.getField("author"));
        assertEquals(Optional.of("testE0, testE1"), entry.getField("editor"));
        assertEquals(Optional.of("testO"), entry.getField("pages"));
    }

    @Test
    public void testImportEntriesBookExample() throws IOException, URISyntaxException {
        Path file = Paths.get(EndnoteImporterTest.class.getResource("Endnote.book.example.enw").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry entry = bibEntries.get(0);

        assertEquals(1, bibEntries.size());
        assertEquals("book", entry.getType());
        assertEquals(Optional.of("Heidelberg"), entry.getField("address"));
        assertEquals(Optional.of("Preißel, René and Stachmann, Bjørn"), entry.getField("author"));
        assertEquals(Optional.of("3., aktualisierte und erweiterte Auflage"), entry.getField("edition"));
        assertEquals(Optional.of("Versionsverwaltung"), entry.getField("keywords"));
        assertEquals(Optional.of("XX, 327"), entry.getField("pages"));
        assertEquals(Optional.of("dpunkt.verlag"), entry.getField("publisher"));
        assertEquals(Optional.of("Git : dezentrale Versionsverwaltung im Team : Grundlagen und Workflows"),
                entry.getField("title"));
        assertEquals(Optional.of("http://d-nb.info/107601965X"), entry.getField("url"));
        assertEquals(Optional.of("2016"), entry.getField("year"));
    }
}
