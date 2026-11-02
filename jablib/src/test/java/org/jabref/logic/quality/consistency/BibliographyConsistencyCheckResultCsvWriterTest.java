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

class BibliographyConsistencyCheckResultCsvWriterTest {

    private final BibtexImporter importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());

    private final BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();

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
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        Path csvFile = tempDir.resolve("checkSimpleLibrary-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key,Journal,Pages,Publisher,Title,Year
                Article,first,-,o,-,-,-
                Article,second,-,-,?,-,-
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    void entriesMissingOnlyRequiredFieldsShowTheseFields(@TempDir Path tempDir) throws IOException {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author Two");
        BibDatabase database = new BibDatabase(List.of(first, second));
        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        Path csvFile = tempDir.resolve("entriesMissingOnlyRequiredFieldsShowTheseFields-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter csvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true, entryTypesManager, bibContext.getMode())) {
            csvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key,Journal,Title,Year
                Article,first,-,-,-
                Article,second,-,-,-
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    void requiredFieldSatisfiedByAliasIsShownAsPresent(@TempDir Path tempDir) throws IOException {
        // In BibLaTeX, `date` is required for articles and `year` is its alias
        BibEntry withYear = new BibEntry(StandardEntryType.Article, "withYear")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.TITLE, "Title")
                .withField(StandardField.JOURNALTITLE, "Journal")
                .withField(StandardField.YEAR, "2024");
        BibEntry withoutDate = new BibEntry(StandardEntryType.Article, "withoutDate")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.TITLE, "Title")
                .withField(StandardField.JOURNALTITLE, "Journal");
        BibDatabaseContext bibContext = new BibDatabaseContext(new BibDatabase(List.of(withYear, withoutDate)));
        bibContext.setMode(BibDatabaseMode.BIBLATEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        Path csvFile = tempDir.resolve("requiredFieldSatisfiedByAliasIsShownAsPresent-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter csvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true, entryTypesManager, bibContext.getMode())) {
            csvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key,Date,Year
                Article,withYear,x,?
                Article,withoutDate,-,-
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    void checkDifferentOutputSymbols(@TempDir Path tempDir) throws IOException {
        UnknownField customField = new UnknownField("custom");
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One") // required
                .withField(StandardField.TITLE, "Title") // required
                .withField(StandardField.PAGES, "some pages") // optional
                .withField(customField, "custom"); // unknown
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One");
        BibDatabase database = new BibDatabase();
        database.insertEntry(first);
        database.insertEntry(second);

        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        Path csvFile = tempDir.resolve("checkDifferentOutputSymbols-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }

        assertEquals("""
                entry type,citation key,custom,Journal,Pages,Title,Year
                Article,first,?,-,o,x,-
                Article,second,-,-,-,-,-
                """, Files.readString(csvFile).replace("\r\n", "\n"));
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
        BibEntry fifth = new BibEntry(StandardEntryType.InProceedings, "fifth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.YEAR, "2024");

        BibDatabase database = new BibDatabase();
        database.insertEntry(first);
        database.insertEntry(second);
        database.insertEntry(third);
        database.insertEntry(fourth);
        database.insertEntry(fifth);

        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        Path csvFile = tempDir.resolve("checkSimpleLibrary-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key,Booktitle,Journal,Location,Pages,Publisher,Title,Year
                Article,first,-,-,-,o,-,-,-
                Article,second,-,-,-,-,?,-,-
                InProceedings,fifth,-,-,-,-,-,-,x
                InProceedings,fourth,-,-,-,-,o,-,x
                InProceedings,third,-,-,?,o,-,-,x
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    void checkLibraryWithoutIssues(@TempDir Path tempDir) throws IOException {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.TITLE, "some title")
                .withField(StandardField.JOURNAL, "some journal")
                .withField(StandardField.YEAR, "2025")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.TITLE, "some title")
                .withField(StandardField.JOURNAL, "some journal")
                .withField(StandardField.YEAR, "2026")
                .withField(StandardField.PAGES, "some pages");

        BibDatabase database = new BibDatabase();
        database.insertEntry(first);
        database.insertEntry(second);

        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        Path csvFile = tempDir.resolve("checkLibraryWithoutIssues-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    @Disabled("This test is only for manual generation of a report")
    void checkManualInput() throws IOException {
        Path file = Path.of("C:\\TEMP\\JabRef\\biblio-anon.bib");
        Path csvFile = file.resolveSibling("biblio-cited.csv");
        BibDatabaseContext databaseContext = importer.importDatabase(file).getDatabaseContext();
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(databaseContext, entryTypesManager, (_, _) -> {
        });
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer, true)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
    }
}
