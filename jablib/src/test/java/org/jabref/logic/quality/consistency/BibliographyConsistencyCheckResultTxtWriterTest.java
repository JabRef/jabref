package org.jabref.logic.quality.consistency;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BibliographyConsistencyCheckResultTxtWriterTest {
    private BibtexImporter importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());

    @Test
    void checkSimpleLibrary(@TempDir Path tempDir) throws Exception {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PUBLISHER, "publisher");
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second));

        Path txtFile = tempDir.resolve("checkSimpleLibrary-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter BibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, false)) {
            BibliographyConsistencyCheckResultTxtWriter.writeFindings();
        }
        assertEquals("""
                Field Presence Consistency Check Result

                | entry type | citation key | Pages | Publisher |
                | ---------- | ------------ | ----- | --------- |
                | Article    | first        | o     | -         |
                | Article    | second       | -     | ?         |

                x | required field is present
                o | optional field is present
                ? | unknown field is present
                - | field is absent
                """, Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    void checkDifferentOutputSymbols(@TempDir Path tempDir) throws Exception {
        UnknownField customField = new UnknownField("custom");
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One") // required
                .withField(StandardField.TITLE, "Title") // required
                .withField(StandardField.PAGES, "some pages") // optional
                .withField(customField, "custom"); // unknown
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One");
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second));

        Path txtFile = tempDir.resolve("checkDifferentOutputSymbols-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter BibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, false)) {
            BibliographyConsistencyCheckResultTxtWriter.writeFindings();
        }
        assertEquals("""
                Field Presence Consistency Check Result

                | entry type | citation key | Custom | Pages | Title |
                | ---------- | ------------ | ------ | ----- | ----- |
                | Article    | first        | ?      | o     | x     |

                x | required field is present
                o | optional field is present
                ? | unknown field is present
                - | field is absent
                """, Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    void checkComplexLibrary(@TempDir Path tempDir) throws Exception {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PUBLISHER, "publisher");

        BibEntry third = new BibEntry(StandardEntryType.InProceedings, "third")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.LOCATION, "location")
                .withField(StandardField.YEAR, "2024")
                .withField(StandardField.PAGES, "some pages");
        BibEntry fourth = new BibEntry(StandardEntryType.InProceedings, "fourth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.YEAR, "2024")
                .withField(StandardField.PUBLISHER, "publisher");
        BibEntry fifth = new BibEntry(StandardEntryType.InProceedings, "fifth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.YEAR, "2024");

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second, third, fourth, fifth));

        Path txtFile = tempDir.resolve("checkSimpleLibrary-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter BibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, false)) {
            BibliographyConsistencyCheckResultTxtWriter.writeFindings();
        }
        assertEquals("""
                Field Presence Consistency Check Result

                | entry type    | citation key  | Location | Pages | Publisher |
                | ------------- | ------------- | -------- | ----- | --------- |
                | Article       | first         | -        | o     | -         |
                | Article       | second        | -        | -     | ?         |
                | InProceedings | fourth        | -        | -     | o         |
                | InProceedings | third         | ?        | o     | -         |

                x | required field is present
                o | optional field is present
                ? | unknown field is present
                - | field is absent
                """, Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    void checkLibraryWithoutIssuesWithOutPorcelain(@TempDir Path tempDir) throws Exception {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second));

        Path txtFile = tempDir.resolve("checkLibraryWithoutIssues-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter BibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, false)) {
            BibliographyConsistencyCheckResultTxtWriter.writeFindings();
        }
        assertEquals("""
                Field Presence Consistency Check Result

                No errors found.
                """, Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    void checkLibraryWithoutIssuesWithPorcelain(@TempDir Path tempDir) throws Exception {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second));

        Path txtFile = tempDir.resolve("checkLibraryWithoutIssues-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter BibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, true)) {
            BibliographyConsistencyCheckResultTxtWriter.writeFindings();
        }
        assertEquals("", Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    @Disabled("This test is only for manual generation of a report")
    void checkManualInput() throws Exception {
        Path file = Path.of("C:\\TEMP\\JabRef\\biblio-anon.bib");
        Path txtFile = file.resolveSibling("biblio-cited.txt");
        BibDatabaseContext databaseContext = importer.importDatabase(file).getDatabaseContext();
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(databaseContext.getEntries());
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter BibliographyConsistencyCheckResultTxtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, true)) {
            BibliographyConsistencyCheckResultTxtWriter.writeFindings();
        }
    }
}
