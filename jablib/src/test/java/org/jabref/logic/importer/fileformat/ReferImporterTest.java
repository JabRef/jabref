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

    @Test
    void importMultipleEntries() throws IOException, URISyntaxException {
        Path file = Path.of(ReferImporterTest.class.getResource("refer.bibIX.ref").toURI());
        List<BibEntry> bibEntries = referImporter.importDatabase(file).getDatabase().getEntries();

        BibEntry bookEntry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "AuthorL, AuthorsF I.")
                .withField(StandardField.TITLE, "TitleTest")
                .withField(StandardField.SERIES, "Series")
                .withField(StandardField.VOLUME, "Volume")
                .withField(StandardField.ADDRESS, "Place")
                .withField(StandardField.PUBLISHER, "Publisher")
                .withField(StandardField.NOTE, "Loc. in Archive B; Rights B; Call Number B")
                .withField(StandardField.ISBN, "I-S-B-N")
                .withField(StandardField.URL, "URL")
                .withField(StandardField.EDITION, "Ed")
                .withField(StandardField.ABSTRACT, "Abstract")
                .withField(StandardField.LANGUAGE, "en")
                .withField(StandardField.YEAR, "Date");

        BibEntry conferencePaperEntry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.TITLE, "PTitle")
                .withField(StandardField.SERIES, "Series")
                .withField(StandardField.VOLUME, "Volume")
                .withField(StandardField.ADDRESS, "PlaceAddress")
                .withField(StandardField.PUBLISHER, "Publisher")
                .withField(StandardField.PAGES, "Pages")
                .withField(StandardField.NOTE, "Loc. in Archive CP; Rights CP; Call Number CP")
                .withField(StandardField.ISBN, "ISBN")
                .withField(StandardField.URL, "URL")
                .withField(StandardField.ABSTRACT, "Abstract")
                .withField(StandardField.BOOKTITLE, "Proceedings Title")
                .withField(StandardField.AUTHOR, "AuthorL, AuthorsF I. and ContributorL, ContributorF I. and Series EditorL, SeriesF I.")
                .withField(StandardField.EDITOR, "EditorL, EditorF I.")
                .withField(StandardField.TRANSLATOR, "TranslatorL, TranslatorF I")
                .withField(StandardField.YEAR, "DateD");

        BibEntry encyclopediaEntry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, "Title")
                .withField(StandardField.SERIES, "Series")
                .withField(StandardField.VOLUME, "Volume")
                .withField(StandardField.ADDRESS, "Place")
                .withField(StandardField.PUBLISHER, "Publisher")
                .withField(StandardField.PAGES, "Pages")
                .withField(StandardField.NOTE, "Loc. in Archive Ep; Rights Ep; Call Number Ep")
                .withField(StandardField.ISBN, "ISBN")
                .withField(StandardField.URL, "URL")
                .withField(StandardField.EDITION, "Edition")
                .withField(StandardField.ABSTRACT, "Abstract")
                .withField(StandardField.BOOKTITLE, "Encyclopedia Title")
                .withField(StandardField.AUTHOR, "AuthorL, AuthorsF I. and ContributorL, ContributorF I. and Series EditorL, SeriesF I.")
                .withField(StandardField.EDITOR, "EditorL, EditorF I.")
                .withField(StandardField.TRANSLATOR, "TranslatorL, TranslatorF I")
                .withField(StandardField.YEAR, "Date");

        BibEntry thesisEntry = new BibEntry(StandardEntryType.PhdThesis)
                .withField(StandardField.TITLE, "Title")
                .withField(StandardField.ADDRESS, "Place")
                .withField(StandardField.SCHOOL, "University")
                .withField(StandardField.DOI, "Type")
                .withField(StandardField.NOTE, "Loc. in Archive Th; Rights Th; Call Number")
                .withField(StandardField.URL, "Url")
                .withField(StandardField.ABSTRACT, "Abstract")
                .withField(StandardField.AUTHOR, "AuthorL, AuthorsF I. and ContributorL, ContributorF I.")
                .withField(StandardField.YEAR, "Date");

        assertEquals(4, bibEntries.size());
        assertEquals(bookEntry, bibEntries.getFirst());
        assertEquals(conferencePaperEntry, bibEntries.get(1));
        assertEquals(encyclopediaEntry, bibEntries.get(2));
        assertEquals(thesisEntry, bibEntries.get(3));
    }
}
