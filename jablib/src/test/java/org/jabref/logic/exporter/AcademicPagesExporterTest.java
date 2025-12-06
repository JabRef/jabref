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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AcademicPagesExporterTest {
    private AcademicPagesExporter exporter;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        exporter = new AcademicPagesExporter(mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS), new SelfContainedSaveOrder(SaveOrder.OrderType.SPECIFIED, List.of()));
        databaseContext = new BibDatabaseContext();
    }

    @Test
    void exportArticleWithFullDateAndRequiredFieldsGeneratesCorrectFileNameAndContent(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("testKey")
                                                                .withField(StandardField.TITLE, "Test Title")
                                                                .withField(StandardField.AUTHOR, "Test Author")
                                                                .withField(StandardField.YEAR, "2023")
                                                                .withField(StandardField.MONTH, "05")
                                                                .withField(StandardField.DAY, "12")
                                                                .withField(StandardField.JOURNAL, "Test Journal");

        exporter.export(databaseContext, tempDir, List.of(entry));

        // Verify file name follows pattern: title.md (SafeFileName)
        Path expectedFile = tempDir.resolve("Test-Title.md");
        assertTrue(Files.exists(expectedFile));

        List<String> expected = List.of("---",
                "title: \"Test Title\"",
                "collection: publications",
                "category: Article",
                "permalink: /publication/Test-Title",
                "excerpt: ''",
                "date: 2023-05-12",
                "venue: 'Test Journal'",
                "slidesurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "paperurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "bibtexurl: 'https://[insert username].github.io/files/[insert filename].bib'",
                "citation: 'Test Author. (2023). \"&quot;Test Title.&quot; <i>Test Journal</i>.'",
                "---",
                "");
        assertEquals(expected, Files.readAllLines(expectedFile));
    }

    @Test
    void exportArticleWithMissingMonthAndDayDefaultsToJanuaryFirst(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("testKey")
                                                                .withField(StandardField.TITLE, "No Date")
                                                                .withField(StandardField.YEAR, "2023");
        exporter.export(databaseContext, tempDir, List.of(entry));

        // Expect default date 2023-01-01
        Path expectedFile = tempDir.resolve("No-Date.md");
        assertTrue(Files.exists(expectedFile));

        List<String> expected = List.of("---",
                "title: \"No Date\"",
                "collection: publications",
                "category: Article",
                "permalink: /publication/No-Date",
                "excerpt: ''",
                "date: 2023-01-01",
                "venue: 'Unknown'",
                "slidesurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "paperurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "bibtexurl: 'https://[insert username].github.io/files/[insert filename].bib'",
                "citation: '. (2023). \"&quot;No Date.&quot; <i></i>.'",
                "---",
                "");
        assertEquals(expected, Files.readAllLines(expectedFile));
    }

    @Test
    void exportArticleWithAbstractAppendsAbstractAfterYamlFrontMatter(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("testKey")
                                                                .withField(StandardField.TITLE, "Abstract Paper")
                                                                .withField(StandardField.YEAR, "2023")
                                                                .withField(StandardField.ABSTRACT, "This is a test abstract.");
        exporter.export(databaseContext, tempDir, List.of(entry));

        Path expectedFile = tempDir.resolve("Abstract-Paper.md");
        assertTrue(Files.exists(expectedFile));

        List<String> expected = List.of("---",
                "title: \"Abstract Paper\"",
                "collection: publications",
                "category: Article",
                "permalink: /publication/Abstract-Paper",
                "excerpt: ''",
                "date: 2023-01-01",
                "venue: 'Unknown'",
                "slidesurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "paperurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "bibtexurl: 'https://[insert username].github.io/files/[insert filename].bib'",
                "citation: '. (2023). \"&quot;Abstract Paper.&quot; <i></i>.'",
                "---",
                "This is a test abstract.");

        assertEquals(expected, Files.readAllLines(expectedFile));
    }

    @Test
    void exportMultipleEntriesGeneratesMultipleIndividualMarkdownFiles(@TempDir Path tempDir) throws Exception {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article).withCitationKey("key1")
                                                                 .withField(StandardField.TITLE, "Paper One")
                                                                 .withField(StandardField.YEAR, "2023");

        BibEntry entry2 = new BibEntry(StandardEntryType.Book).withCitationKey("key2")
                                                              .withField(StandardField.TITLE, "Book Two")
                                                              .withField(StandardField.YEAR, "2022");

        exporter.export(databaseContext, tempDir, List.of(entry1, entry2));

        // Verify both files exist
        Path file1 = tempDir.resolve("Paper-One.md");
        Path file2 = tempDir.resolve("Book-Two.md");
        assertTrue(Files.exists(file1));
        assertTrue(Files.exists(file2));

        List<String> expected1 = List.of("---",
                "title: \"Paper One\"",
                "collection: publications",
                "category: Article",
                "permalink: /publication/Paper-One",
                "excerpt: ''",
                "date: 2023-01-01",
                "venue: 'Unknown'",
                "slidesurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "paperurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "bibtexurl: 'https://[insert username].github.io/files/[insert filename].bib'",
                "citation: '. (2023). \"&quot;Paper One.&quot; <i></i>.'",
                "---",
                "");
        assertEquals(expected1, Files.readAllLines(file1));

        List<String> expected2 = List.of("---",
                "title: \"Book Two\"",
                "collection: publications",
                "category: Book",
                "permalink: /publication/Book-Two",
                "excerpt: ''",
                "date: 2022-01-01",
                "venue: 'Unknown'",
                "slidesurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "paperurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "bibtexurl: 'https://[insert username].github.io/files/[insert filename].bib'",
                "citation: '. (2022). \"&quot;Book Two.&quot; <i></i>.'",
                "---",
                "");
        assertEquals(expected2, Files.readAllLines(file2));
    }

    @Test
    void exportInProceedingsWithBooktitleUsesBooktitleAsVenueAlias(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings).withCitationKey("testKey")
                                                                      .withField(StandardField.TITLE, "Conference Paper")
                                                                      .withField(StandardField.YEAR, "2023")
                                                                      .withField(StandardField.BOOKTITLE, "Conference Proceedings");

        exporter.export(databaseContext, tempDir, List.of(entry));

        Path expectedFile = tempDir.resolve("Conference-Paper.md");
        assertTrue(Files.exists(expectedFile));

        List<String> expected = List.of("---",
                "title: \"Conference Paper\"",
                "collection: publications",
                "category: InProceedings",
                "permalink: /publication/Conference-Paper",
                "excerpt: ''",
                "date: 2023-01-01",
                "venue: 'Unknown'",
                "slidesurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "paperurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "bibtexurl: 'https://[insert username].github.io/files/[insert filename].bib'",
                "citation: '. (2023). \"&quot;Conference Paper.&quot; <i></i>.'",
                "---",
                "");
        assertEquals(expected, Files.readAllLines(expectedFile));
    }

    @Test
    void exportArticleWithSpecialCharactersInTitleGeneratesSafeFileName(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("testKey")
                                                                .withField(StandardField.TITLE, "test title \\/:*?\"<>|")
                                                                .withField(StandardField.YEAR, "2024");

        exporter.export(databaseContext, tempDir, List.of(entry));

        // Resulting file name should be safe: test-title-.md
        Path expectedFile = tempDir.resolve("test-title-.md");
        assertTrue(Files.exists(expectedFile));

        List<String> expected = List.of("---",
                "title: \"test title /:*?\"<>|\"",
                "collection: publications",
                "category: Article",
                "permalink: /publication/test-title-",
                "excerpt: ''",
                "date: 2024-01-01",
                "venue: 'Unknown'",
                "slidesurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "paperurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "bibtexurl: 'https://[insert username].github.io/files/[insert filename].bib'",
                "citation: '. (2024). \"&quot;test title /:*?\"<>|.&quot; <i></i>.'",
                "---",
                "");
        assertEquals(expected, Files.readAllLines(expectedFile));
    }

    @Test
    void exportArticleWithFileFieldGeneratesSlidesAndPaperUrls(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("testKey")
                                                                .withField(StandardField.TITLE, "test title")
                                                                .withField(StandardField.YEAR, "2024")
                                                                .withField(StandardField.FILE, ":arxiv.pdf:PDF");
        exporter.export(databaseContext, tempDir, List.of(entry));

        Path expectedFile = tempDir.resolve("test-title.md");
        assertTrue(Files.exists(expectedFile));

        List<String> expected = List.of("---",
                "title: \"test title\"",
                "collection: publications",
                "category: Article",
                "permalink: /publication/test-title",
                "excerpt: ''",
                "date: 2024-01-01",
                "venue: 'Unknown'",
                "slidesurl: 'arxiv.pdf'",
                "paperurl: 'arxiv.pdf'",
                "bibtexurl: 'https://[insert username].github.io/files/[insert filename].bib'",
                "citation: '. (2024). \"&quot;test title.&quot; <i></i>.'",
                "---",
                "");
        assertEquals(expected, Files.readAllLines(expectedFile));
    }

    @Test
    void exportArticleWithNoTitleThrowsSaveException(@TempDir Path tempDir) {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("testKey");
        assertThrows(SaveException.class, () -> exporter.export(databaseContext, tempDir, List.of(entry)));
    }

    @Test
    void exportArticleWithSpacesInTitleReplacesSpacesWithDashesInPermalink(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("testKey")
                                                                .withField(StandardField.TITLE, "test   title")
                                                                .withField(StandardField.YEAR, "2024")
                                                                .withField(StandardField.MONTH, "01")
                                                                .withField(StandardField.DAY, "01");
        exporter.export(databaseContext, tempDir, List.of(entry));

        // Resulting file name should replace spaces with dashes
        Path expectedFile = tempDir.resolve("test---title.md");
        assertTrue(Files.exists(expectedFile));

        List<String> expected = List.of("---",
                "title: \"test   title\"",
                "collection: publications",
                "category: Article",
                "permalink: /publication/test---title",
                "excerpt: ''",
                "date: 2024-01-01",
                "venue: 'Unknown'",
                "slidesurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "paperurl: 'https://[insert username].github.io/files/[insert filename].pdf'",
                "bibtexurl: 'https://[insert username].github.io/files/[insert filename].bib'",
                "citation: '. (2024). \"&quot;test   title.&quot; <i></i>.'",
                "---",
                "");

        assertEquals(expected, Files.readAllLines(expectedFile));
    }
}
