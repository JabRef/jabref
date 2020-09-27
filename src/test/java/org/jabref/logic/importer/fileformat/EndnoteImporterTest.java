package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

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
        assertEquals(StandardFileType.ENDNOTE, importer.getFileType());
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
            Path file = Path.of(EndnoteImporterTest.class.getResource(string).toURI());
            assertTrue(importer.isRecognizedFormat(file, StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testIsRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> list = Arrays.asList("IEEEImport1.txt", "IsiImporterTest1.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi", "RisImporterTest1.ris",
                "Endnote.pattern.no_enw", "empty.pdf", "annotated.pdf");

        for (String string : list) {
            Path file = Path.of(EndnoteImporterTest.class.getResource(string).toURI());
            assertFalse(importer.isRecognizedFormat(file, Charset.defaultCharset()));
        }
    }

    @Test
    public void testImportEntries0() throws IOException, URISyntaxException {
        Path file = Path.of(EndnoteImporterTest.class.getResource("Endnote.entries.enw").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        assertEquals(5, bibEntries.size());

        BibEntry first = bibEntries.get(0);
        assertEquals(StandardEntryType.Misc, first.getType());
        assertEquals(Optional.of("testA0 and testA1"), first.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("testE0 and testE1"), first.getField(StandardField.EDITOR));
        assertEquals(Optional.of("testT"), first.getField(StandardField.TITLE));

        BibEntry second = bibEntries.get(1);
        assertEquals(StandardEntryType.Misc, second.getType());
        assertEquals(Optional.of("testC"), second.getField(StandardField.ADDRESS));
        assertEquals(Optional.of("testB2"), second.getField(StandardField.BOOKTITLE));
        assertEquals(Optional.of("test8"), second.getField(StandardField.DATE));
        assertEquals(Optional.of("test7"), second.getField(StandardField.EDITION));
        assertEquals(Optional.of("testJ"), second.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("testD"), second.getField(StandardField.YEAR));

        BibEntry third = bibEntries.get(2);
        assertEquals(StandardEntryType.Article, third.getType());
        assertEquals(Optional.of("testB0"), third.getField(StandardField.JOURNAL));

        BibEntry fourth = bibEntries.get(3);
        assertEquals(StandardEntryType.Book, fourth.getType());
        assertEquals(Optional.of("testI0"), fourth.getField(StandardField.PUBLISHER));
        assertEquals(Optional.of("testB1"), fourth.getField(StandardField.SERIES));

        BibEntry fifth = bibEntries.get(4);
        assertEquals(StandardEntryType.MastersThesis, fifth.getType());
        assertEquals(Optional.of("testX"), fifth.getField(StandardField.ABSTRACT));
        assertEquals(Optional.of("testF"), fifth.getCitationKey());
        assertEquals(Optional.of("testR"), fifth.getField(StandardField.DOI));
        assertEquals(Optional.of("testK"), fifth.getField(StandardField.KEYWORDS));
        assertEquals(Optional.of("testO1"), fifth.getField(StandardField.NOTE));
        assertEquals(Optional.of("testN"), fifth.getField(StandardField.NUMBER));
        assertEquals(Optional.of("testP"), fifth.getField(StandardField.PAGES));
        assertEquals(Optional.of("testI1"), fifth.getField(StandardField.SCHOOL));
        assertEquals(Optional.of("testU"), fifth.getField(StandardField.URL));
        assertEquals(Optional.of("testV"), fifth.getField(StandardField.VOLUME));
    }

    @Test
    public void testImportEntries1() throws IOException {
        String medlineString = "%O Artn\\\\s testO\n%A testA,\n%E testE0, testE1";
        List<BibEntry> bibEntries = importer.importDatabase(new BufferedReader(new StringReader(medlineString))).getDatabase()
                                            .getEntries();

        BibEntry entry = bibEntries.get(0);

        assertEquals(1, bibEntries.size());
        assertEquals(StandardEntryType.Misc, entry.getType());
        assertEquals(Optional.of("testA"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("testE0, testE1"), entry.getField(StandardField.EDITOR));
        assertEquals(Optional.of("testO"), entry.getField(StandardField.PAGES));
    }

    @Test
    public void testImportEntriesBookExample() throws IOException, URISyntaxException {
        Path file = Path.of(EndnoteImporterTest.class.getResource("Endnote.book.example.enw").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file, StandardCharsets.UTF_8).getDatabase().getEntries();

        BibEntry entry = bibEntries.get(0);

        assertEquals(1, bibEntries.size());
        assertEquals(StandardEntryType.Book, entry.getType());
        assertEquals(Optional.of("Heidelberg"), entry.getField(StandardField.ADDRESS));
        assertEquals(Optional.of("Preißel, René and Stachmann, Bjørn"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("3., aktualisierte und erweiterte Auflage"), entry.getField(StandardField.EDITION));
        assertEquals(Optional.of("Versionsverwaltung"), entry.getField(StandardField.KEYWORDS));
        assertEquals(Optional.of("XX, 327"), entry.getField(StandardField.PAGES));
        assertEquals(Optional.of("dpunkt.verlag"), entry.getField(StandardField.PUBLISHER));
        assertEquals(Optional.of("Git : dezentrale Versionsverwaltung im Team : Grundlagen und Workflows"),
                entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("http://d-nb.info/107601965X"), entry.getField(StandardField.URL));
        assertEquals(Optional.of("2016"), entry.getField(StandardField.YEAR));
    }
}
