package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AcademicPagesExporterTest {

    private AcademicPagesExporter exporter;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        exporter = new AcademicPagesExporter(
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                new SelfContainedSaveOrder(SaveOrder.OrderType.SPECIFIED, List.of()));
        databaseContext = new BibDatabaseContext();
    }

    @Test
    void exportArticleWithFullDateAndRequiredFieldsGeneratesCorrectFileNameAndContent(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("testKey")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.YEAR, "2023")
                .withField(StandardField.MONTH, "05")
                .withField(StandardField.DAY, "12")
                .withField(StandardField.JOURNAL, "Test Journal");

        exporter.export(databaseContext, tempDir, List.of(entry));

        // Verify file name follows pattern: YYYY-MM-DD-title.md
        Path expectedFile = tempDir.resolve("2023-05-12-test-title.md");
        assertTrue(Files.exists(expectedFile));

        String content = Files.readString(expectedFile);

        // Verify YAML front matter fields
        assertTrue(content.contains("title: \"Test Title\""));
        assertTrue(content.contains("date: 2023-05-12"));
        assertTrue(content.contains("venue: 'Test Journal'"));
        assertTrue(content.contains("citation: 'Test Author (2023). \"Test Title.\" <i>Test Journal</i>.'"));
    }

    @Test
    void exportArticleWithMissingMonthAndDayDefaultsToJanuaryFirst(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("testKey")
                .withField(StandardField.TITLE, "No Date")
                .withField(StandardField.YEAR, "2023");

        exporter.export(databaseContext, tempDir, List.of(entry));

        // Expect default date 2023-01-01
        Path expectedFile = tempDir.resolve("2023-01-01-no-date.md");
        assertTrue(Files.exists(expectedFile));

        String content = Files.readString(expectedFile);
        assertTrue(content.contains("date: 2023-01-01"));
    }

    @Test
    void exportArticleWithAbstractAppendsAbstractAfterYamlFrontMatter(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("testKey")
                .withField(StandardField.TITLE, "Abstract Paper")
                .withField(StandardField.YEAR, "2023")
                .withField(StandardField.ABSTRACT, "This is a test abstract.");

        exporter.export(databaseContext, tempDir, List.of(entry));

        Path expectedFile = tempDir.resolve("2023-01-01-abstract-paper.md");
        assertTrue(Files.exists(expectedFile));

        String content = Files.readString(expectedFile);

        // Abstract should be outside the YAML block (after the second '---')
        assertTrue(content.contains("---"));
        assertTrue(content.endsWith("\nThis is a test abstract.\n"));
    }

    @Test
    void exportMultipleEntriesGeneratesMultipleIndividualMarkdownFiles(@TempDir Path tempDir) throws Exception {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("key1")
                .withField(StandardField.TITLE, "Paper One")
                .withField(StandardField.YEAR, "2023");

        BibEntry entry2 = new BibEntry(StandardEntryType.Book)
                .withCitationKey("key2")
                .withField(StandardField.TITLE, "Book Two")
                .withField(StandardField.YEAR, "2022");

        exporter.export(databaseContext, tempDir, List.of(entry1, entry2));

        // Verify both files exist
        assertTrue(Files.exists(tempDir.resolve("2023-01-01-paper-one.md")));
        assertTrue(Files.exists(tempDir.resolve("2022-01-01-book-two.md")));
    }

    @Test
    void exportInProceedingsWithBooktitleUsesBooktitleAsVenueAlias(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("testKey")
                .withField(StandardField.TITLE, "Conference Paper")
                .withField(StandardField.YEAR, "2023")
                .withField(StandardField.BOOKTITLE, "Conference Proceedings");

        exporter.export(databaseContext, tempDir, List.of(entry));

        Path expectedFile = tempDir.resolve("2023-01-01-conference-paper.md");
        assertTrue(Files.exists(expectedFile));

        String content = Files.readString(expectedFile);

        // 'venue' should be populated from 'booktitle' since 'journal' is missing
        assertTrue(content.contains("venue: 'Conference Proceedings'"));
    }
}
