package org.jabref.logic.quality.consistency;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
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
    private final BibtexImporter importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());

    @Test
    void checkSimpleLibrary(@TempDir Path tempDir) throws IOException {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PUBLISHER, "publisher");
        BibDatabase database = new BibDatabase();
        database.insertEntry(first);
        database.insertEntry(second);

        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, (count, total) -> {
        });

        Path txtFile = tempDir.resolve("checkSimpleLibrary-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter txtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, false, new BibEntryTypesManager(), bibContext.getMode())) {
            txtWriter.writeFindings();
        }
        assertEquals("""
                Field Presence Consistency Check Result

                | entry type | citation key | Pages | Publisher |
                | ---------- | ------------ | ----- | --------- |
                | Article    | first        | o     | -         |
                | Article    | second       | -     | ?         |

                | Symbol | Meaning                   |
                | ------ | ------------------------- |
                | x      | required field is present |
                | o      | optional field is present |
                | ?      | unknown field is present  |
                | -      | field is absent           |
                """, Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    void entriesMissingRequiredFieldsAreReported(@TempDir Path tempDir) throws Exception {
        BibEntry withDate = new BibEntry(StandardEntryType.Online)
                .withCitationKey("withDate")
                // required field
                .withField(StandardField.DATE, "date")
                // optional field
                .withField(StandardField.URLDATE, "urldate");
        BibEntry withoutDate = new BibEntry(StandardEntryType.Online)
                .withCitationKey("withoutDate")
                .withField(StandardField.URLDATE, "urldate");

        List<BibEntry> bibEntriesList = List.of(withDate, withoutDate);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntries(bibEntriesList);

        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);
        bibContext.setMode(BibDatabaseMode.BIBLATEX);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(bibContext, (_, _) -> {
                });

        Path txtFile = tempDir.resolve("checkSimpleLibrary-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter txtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, false, new BibEntryTypesManager(), bibContext.getMode())) {
            txtWriter.writeFindings();
        }

        assertEquals("""
                Field Presence Consistency Check Result

                | entry type | citation key | Date |
                | ---------- | ------------ | ---- |
                | Online     | withDate     | x    |
                | Online     | withoutDate  | -    |

                | Symbol | Meaning                   |
                | ------ | ------------------------- |
                | x      | required field is present |
                | o      | optional field is present |
                | ?      | unknown field is present  |
                | -      | field is absent           |
                """, Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    void checkDifferentOutputSymbols(@TempDir Path tempDir) throws IOException {
        UnknownField customField = new UnknownField("custom");
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One") // required
                .withField(StandardField.TITLE, "Title")       // required
                .withField(StandardField.PAGES, "some pages")  // optional
                .withField(customField, "custom");             // unknown
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One");
        List<BibEntry> bibEntriesList = List.of(first, second);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntries(bibEntriesList);
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);
        bibContext.setMode(BibDatabaseMode.BIBTEX);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, (_, _) -> {
        });

        Path txtFile = tempDir.resolve("checkDifferentOutputSymbols-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter txtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, false, new BibEntryTypesManager(), bibContext.getMode())) {
            txtWriter.writeFindings();
        }
        assertEquals("""
                Field Presence Consistency Check Result

                | entry type | citation key | custom | Pages | Title |
                | ---------- | ------------ | ------ | ----- | ----- |
                | Article    | first        | ?      | o     | x     |
                | Article    | second       | -      | -     | -     |

                | Symbol | Meaning                   |
                | ------ | ------------------------- |
                | x      | required field is present |
                | o      | optional field is present |
                | ?      | unknown field is present  |
                | -      | field is absent           |
                """, Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    void checkVeryLongCitationKey(@TempDir Path tempDir) throws IOException {
        UnknownField customField = new UnknownField("custom");
        BibEntry first = new BibEntry(StandardEntryType.Article, "first-very-long-key")
                .withField(StandardField.AUTHOR, "Author One") // required
                .withField(StandardField.TITLE, "Title") // required
                .withField(StandardField.PAGES, "some pages") // optional
                .withField(customField, "custom"); // unknown
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One");
        List<BibEntry> bibEntriesList = List.of(first, second);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntries(bibEntriesList);
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, (_, _) -> {
        });

        Path txtFile = tempDir.resolve("checkDifferentOutputSymbols-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter txtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, false, new BibEntryTypesManager(), bibContext.getMode())) {
            txtWriter.writeFindings();
        }
        assertEquals("""
                Field Presence Consistency Check Result

                | entry type | citation key        | custom | Pages | Title |
                | ---------- | ------------------- | ------ | ----- | ----- |
                | Article    | first-very-long-key | ?      | o     | x     |
                | Article    | second              | -      | -     | -     |

                | Symbol | Meaning                   |
                | ------ | ------------------------- |
                | x      | required field is present |
                | o      | optional field is present |
                | ?      | unknown field is present  |
                | -      | field is absent           |
                """, Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    void checkComplexLibrary(@TempDir Path tempDir) throws IOException {
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
        // Entry added to check for alphabetical ordering of citation keys
        BibEntry fifth = new BibEntry(StandardEntryType.InProceedings, "fifth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.LOCATION, "location")
                .withField(StandardField.YEAR, "2024");
        BibEntry sixth = new BibEntry(StandardEntryType.InProceedings, "sixth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.YEAR, "2024");

        List<BibEntry> bibEntriesList = List.of(first, second, third, fourth, fifth, sixth);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntries(bibEntriesList);
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, (_, _) -> {
        });

        Path txtFile = tempDir.resolve("checkSimpleLibrary-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter txtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, false, new BibEntryTypesManager(), bibContext.getMode())) {
            txtWriter.writeFindings();
        }
        assertEquals("""
                Field Presence Consistency Check Result

                | entry type    | citation key | Location | Pages | Publisher |
                | ------------- | ------------ | -------- | ----- | --------- |
                | Article       | first        | -        | o     | -         |
                | Article       | second       | -        | -     | ?         |
                | InProceedings | fifth        | o        | -     | -         |
                | InProceedings | fourth       | -        | -     | o         |
                | InProceedings | sixth        | -        | -     | -         |
                | InProceedings | third        | o        | o     | -         |

                | Symbol | Meaning                   |
                | ------ | ------------------------- |
                | x      | required field is present |
                | o      | optional field is present |
                | ?      | unknown field is present  |
                | -      | field is absent           |
                """, Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    void checkLibraryWithoutIssuesWithOutPorcelain(@TempDir Path tempDir) throws IOException {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        List<BibEntry> bibEntriesList = List.of(first, second);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntries(bibEntriesList);
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, (_, _) -> {
        });

        Path txtFile = tempDir.resolve("checkLibraryWithoutIssues-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter txtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, false, new BibEntryTypesManager(), bibContext.getMode())) {
            txtWriter.writeFindings();
        }
        assertEquals("""
                Field Presence Consistency Check Result

                No errors found.
                """, Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    void checkLibraryWithoutIssuesWithPorcelain(@TempDir Path tempDir) throws IOException {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        List<BibEntry> bibEntriesList = List.of(first, second);
        BibDatabase bibDatabase = new BibDatabase();
        bibDatabase.insertEntries(bibEntriesList);
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, (_, _) -> {
        });

        Path txtFile = tempDir.resolve("checkLibraryWithoutIssues-result.txt");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter txtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, true)) {
            txtWriter.writeFindings();
        }
        assertEquals("", Files.readString(txtFile).replace("\r\n", "\n"));
    }

    @Test
    @Disabled("This test is only for manual generation of a report")
    void checkManualInput() throws IOException {
        Path file = Path.of("C:\\TEMP\\JabRef\\biblio-anon.bib");
        Path txtFile = file.resolveSibling("biblio-cited.txt");
        BibDatabaseContext databaseContext = importer.importDatabase(file).getDatabaseContext();
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(databaseContext, (_, _) -> {
        });
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(txtFile));
             BibliographyConsistencyCheckResultTxtWriter txtWriter = new BibliographyConsistencyCheckResultTxtWriter(result, writer, true)) {
            txtWriter.writeFindings();
        }
    }
}
