package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReferImporterTest {

    private ReferImporter referImporter;

    @BeforeEach
    void setUp() {
        referImporter = new ReferImporter();
    }

    @Test
    void getFormatName() {
        assertEquals("Refer/BibIX", referImporter.getName());
    }

    @Test
    void getCLIId() {
        assertEquals("refer-bibIX", referImporter.getId());
    }

    @Test
    void getDescription() {
        assertEquals("Import for the Refer/BibIX file.", referImporter.getDescription());
    }

    @Test
    void getExtension() {
        assertEquals(StandardFileType.TXT, referImporter.getFileType());
    }

    @Test
    void isRecognizedFormat() throws IOException, URISyntaxException {
        List<String> list = List.of("refer.bibIX.Journal.ref", "refer.bibIX.ref");

        for (String str : list) {
            Path file = Path.of(ReferImporterTest.class.getResource(str).toURI());
            assertTrue(referImporter.isRecognizedFormat(file));
        }
    }

    @Test
    void isRecognizedFormatReject() throws IOException, URISyntaxException {
        List<String> list = List.of("Endnote.pattern.A.enw", "IEEEImport1.txt", "IsiImporterTest1.isi", "IsiImporterTestInspec.isi",
                "IsiImporterTestWOS.isi", "IsiImporterTestMedline.isi", "RisImporterTest1.ris",
                "Endnote.pattern.no_enw", "empty.pdf", "pdf/annotated.pdf");

        for (String str : list) {
            Path file = Path.of(ReferImporterTest.class.getResource(str).toURI());
            assertFalse(referImporter.isRecognizedFormat(file));
        }
    }

    @Test
    void importDatabaseSingleEntryTest() throws IOException, URISyntaxException {
        Path file = Path.of(ReferImporterTest.class.getResource("refer.bibIX.Journal.ref").toURI());
        List<BibEntry> bibEntryList = referImporter.importDatabase(file).getDatabase().getEntries();
        BibEntry actualEntry = bibEntryList.getFirst();

        BibEntry expectedEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "AuthorL, AuthorsF I. and ContributorL, ContributorF I. and Reviewed AuthorL, ReviewedF I.")
                .withField(StandardField.JOURNAL, "Publication")
                .withField(StandardField.TITLE, "Title")
                .withField(StandardField.VOLUME, "Volume")
                .withField(StandardField.ISSUE, "Issue")
                .withField(StandardField.PAGES, "Pages")
                .withField(StandardField.SERIES, "Series")
                .withField(StandardField.URL, "Url")
                .withField(StandardField.ABSTRACT, "Abstract")
                .withField(StandardField.EDITOR, "EditorL, EditorF I.")
                .withField(StandardField.TRANSLATOR, "TranslatorL, TranslatorF I")
                .withField(StandardField.YEAR, "2025")
                .withField(StandardField.NOTE, "Loc. in Archive; Rights");

        assertEquals(1, bibEntryList.size());
        assertEquals(expectedEntry, actualEntry);
    }

    @Test
    void importMultipleEntries() throws IOException, URISyntaxException {
        Path file = Path.of(ReferImporterTest.class.getResource("refer.bibIX.ref").toURI());
        List<BibEntry> bibEntries = referImporter.importDatabase(file).getDatabase().getEntries();

        assertEquals(5, bibEntries.size());

        BibEntry first = bibEntries.getFirst();
        assertEquals(StandardEntryType.Book, first.getType());
        assertEquals(Optional.of("AuthorL, AuthorsF I."), first.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("TitleTest"), first.getField(StandardField.TITLE));
        assertEquals(Optional.of("en"), first.getField(StandardField.LANGUAGE));
        assertEquals(Optional.of("I-S-B-N"), first.getField(StandardField.ISBN));
        assertEquals(Optional.of("Ed"), first.getField(StandardField.EDITION));

        BibEntry second = bibEntries.get(1);
        assertEquals(StandardEntryType.InProceedings, second.getType());
        assertEquals(Optional.of("PlaceAddress"), second.getField(StandardField.ADDRESS));
        assertEquals(Optional.of("Proceedings Title"), second.getField(StandardField.BOOKTITLE));
        assertEquals(Optional.of("DateD"), second.getField(StandardField.YEAR));
        assertEquals(Optional.of("TranslatorL, TranslatorF I"), second.getField(StandardField.TRANSLATOR));

        BibEntry third = bibEntries.get(2);
        assertEquals(StandardEntryType.Misc, third.getType());
        assertEquals(Optional.of("Encyclopedia Title"), third.getField(StandardField.BOOKTITLE));

        BibEntry fourth = bibEntries.get(3);
        assertEquals(StandardEntryType.Misc, fourth.getType());
        assertEquals(Optional.of("Publisher"), fourth.getField(StandardField.PUBLISHER));
        assertEquals(Optional.of("Series"), fourth.getField(StandardField.SERIES));

        BibEntry fifth = bibEntries.get(4);
        assertEquals(StandardEntryType.PhdThesis, fifth.getType());
        assertEquals(Optional.of("Abstract"), fifth.getField(StandardField.ABSTRACT));
        assertEquals(Optional.of("Type"), fifth.getField(StandardField.DOI));
        assertEquals(Optional.of("University"), fifth.getField(StandardField.SCHOOL));
        assertEquals(Optional.of("Url"), fifth.getField(StandardField.URL));
    }

    @Test
    void editorNameInAuthorField() throws IOException {
        String refEntry = """
                %0 Edited Book
                %A testE
                %A testEL, testEF
                %A editor
                """;

        BibEntry entry = referImporter.importDatabase(new BufferedReader(Reader.of(refEntry)))
                                                   .getDatabase()
                                                   .getEntries()
                                                   .getFirst();

        assertEquals(StandardEntryType.Book, entry.getType());
        assertEquals(Optional.empty(), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("testE and testEL, testEF. and editor"), entry.getField(StandardField.EDITOR));
    }
}
