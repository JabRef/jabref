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

class BibliographyConsistencyCheckResultCsvWriterTest {

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

        Path csvFile = tempDir.resolve("checkSimpleLibrary-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key,Pages,Publisher
                Article,first,o,-
                Article,second,-,?
                """, Files.readString(csvFile).replace("\r\n", "\n"));
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

        Path csvFile = tempDir.resolve("checkDifferentOutputSymbols-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key,Custom,Pages,Title
                Article,first,?,o,x
                """, Files.readString(csvFile).replace("\r\n", "\n"));
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

        Path csvFile = tempDir.resolve("checkSimpleLibrary-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key,Location,Pages,Publisher
                Article,first,-,o,-
                Article,second,-,-,?
                InProceedings,fourth,-,-,o
                InProceedings,third,?,o,-
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    void checkLibraryWithoutIssues(@TempDir Path tempDir) throws Exception {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second));

        Path csvFile = tempDir.resolve("checkLibraryWithoutIssues-result.csv");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
        assertEquals("""
                entry type,citation key
                """, Files.readString(csvFile).replace("\r\n", "\n"));
    }

    @Test
    @Disabled("This test is only for manual generation of a report")
    void checkManualInput() throws Exception {
        Path file = Path.of("C:\\TEMP\\JabRef\\biblio-anon.bib");
        Path csvFile = file.resolveSibling("biblio-cited.csv");
        BibDatabaseContext databaseContext = importer.importDatabase(file).getDatabaseContext();
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(databaseContext.getEntries());
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(csvFile));
             BibliographyConsistencyCheckResultCsvWriter paperConsistencyCheckResultCsvWriter = new BibliographyConsistencyCheckResultCsvWriter(result, writer)) {
            paperConsistencyCheckResultCsvWriter.writeFindings();
        }
    }
}
