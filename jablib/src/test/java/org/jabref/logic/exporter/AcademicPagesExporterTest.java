package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AcademicPagesExporterTest {

    private AcademicPagesExporter exporter;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        exporter = new AcademicPagesExporter(
                mock(FieldPreferences.class, Answers.RETURNS_DEEP_STUBS),
                new BibEntryTypesManager());
        databaseContext = new BibDatabaseContext();
    }

    @Test
    void noEntriesWritesNoFiles(@TempDir Path tempDir) throws Exception {
        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of());
        assertFalse(Files.exists(tempDir.resolve("output")));
    }

    @Test
    void articleCreatesCorrectFile(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("smith2020")
                .withField(StandardField.TITLE, "A Great Paper")
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.JOURNAL, "Nature")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.MONTH, "march")
                .withField(StandardField.ABSTRACT, "This paper is about great things.");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        Path generated = tempDir.resolve("output").resolve("_publications").resolve("2020-03-01-smith2020.md");
        assertTrue(Files.exists(generated));

        String content = Files.readString(generated);
        assertTrue(content.contains("title: \"A Great Paper\""));
        assertTrue(content.contains("collection: publications"));
        assertTrue(content.contains("permalink: /publication/2020-03-01-smith2020"));
        assertTrue(content.contains("date: 2020-03-01"));
        assertTrue(content.contains("venue: 'Nature'"));
        assertTrue(content.contains("category: manuscripts"));
        assertTrue(content.contains("This paper is about great things."));
        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        assertTrue(Files.exists(tempDir.resolve("output").resolve("_publications")));
        assertTrue(Files.exists(tempDir.resolve("output").resolve("files")));
    }

    @Test
    void bibFileGoesIntoFilesDir(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("smith2020")
                .withField(StandardField.TITLE, "A Paper")
                .withField(StandardField.YEAR, "2020");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        assertTrue(Files.exists(tempDir.resolve("output").resolve("files").resolve("smith2020.bib")));
    }

    @Test
    void bibtexUrlHasFilesPrefix(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("smith2020")
                .withField(StandardField.TITLE, "A Paper")
                .withField(StandardField.YEAR, "2020");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output").resolve("_publications").resolve("2020-01-01-smith2020.md"));
        assertTrue(content.contains("bibtexurl: '/files/smith2020.bib'"));
    }

    @Test
    void bookIsManuscripts(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("doe2019book")
                .withField(StandardField.TITLE, "My Book")
                .withField(StandardField.AUTHOR, "Doe, Jane")
                .withField(StandardField.PUBLISHER, "Springer")
                .withField(StandardField.YEAR, "2019");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output").resolve("_publications").resolve("2019-01-01-doe2019book.md"));
        assertTrue(content.contains("category: manuscripts"));
    }

    @Test
    void inProceedingsIsConferences(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("jones2021conf")
                .withField(StandardField.TITLE, "Conference Paper")
                .withField(StandardField.AUTHOR, "Jones, Bob")
                .withField(StandardField.BOOKTITLE, "Proceedings of ICSE")
                .withField(StandardField.YEAR, "2021");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output").resolve("_publications").resolve("2021-01-01-jones2021conf.md"));
        assertTrue(content.contains("category: conferences"));
        assertTrue(content.contains("venue: 'Proceedings of ICSE'"));
    }

    @Test
    void inCollectionIsManuscripts(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.InCollection)
                .withCitationKey("lee2018chapter")
                .withField(StandardField.TITLE, "A Book Chapter")
                .withField(StandardField.AUTHOR, "Lee, Alice")
                .withField(StandardField.BOOKTITLE, "Handbook of AI")
                .withField(StandardField.YEAR, "2018");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output").resolve("_publications").resolve("2018-01-01-lee2018chapter.md"));
        assertTrue(content.contains("category: manuscripts"));
    }

    @Test
    void miscIsManuscripts(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("misc2022")
                .withField(StandardField.TITLE, "Some Misc Entry")
                .withField(StandardField.YEAR, "2022");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output").resolve("_publications").resolve("2022-01-01-misc2022.md"));
        assertTrue(content.contains("category: manuscripts"));
    }

    @Test
    void noteBecomesExcerpt(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("noted2020")
                .withField(StandardField.TITLE, "Paper With Note")
                .withField(StandardField.AUTHOR, "Author, A")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.NOTE, "This is a short excerpt.");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output").resolve("_publications").resolve("2020-01-01-noted2020.md"));
        assertTrue(content.contains("excerpt: 'This is a short excerpt.'"));
    }

    @Test
    void missingNoteOmitsExcerpt(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("nonoted2020")
                .withField(StandardField.TITLE, "Paper Without Note")
                .withField(StandardField.AUTHOR, "Author, A")
                .withField(StandardField.YEAR, "2020");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output").resolve("_publications").resolve("2020-01-01-nonoted2020.md"));
        assertFalse(content.contains("excerpt:"));
    }

    @Test
    void missingMonthDefaultsToJanuary(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("nomonth2021")
                .withField(StandardField.TITLE, "No Month Paper")
                .withField(StandardField.YEAR, "2021");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        assertTrue(Files.exists(tempDir.resolve("output").resolve("_publications").resolve("2021-01-01-nomonth2021.md")));
    }

    @Test
    void dateFieldExtractsYear(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("datetest2021")
                .withField(StandardField.TITLE, "Date Field Test")
                .withField(StandardField.DATE, "2021-08-25");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        Path generated = tempDir.resolve("output").resolve("_publications").resolve("2021-01-01-datetest2021.md");
        assertTrue(Files.exists(generated));
        assertTrue(Files.readString(generated).contains("date: 2021-01-01"));
    }

    @Test
    void multipleEntriesCreateMultipleFiles(@TempDir Path tempDir) throws Exception {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("paper2020")
                .withField(StandardField.TITLE, "Paper One")
                .withField(StandardField.YEAR, "2020");

        BibEntry entry2 = new BibEntry(StandardEntryType.Book)
                .withCitationKey("book2021")
                .withField(StandardField.TITLE, "Book One")
                .withField(StandardField.YEAR, "2021");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry1, entry2));

        Path publicationsDir = tempDir.resolve("output").resolve("_publications");
        assertTrue(Files.exists(publicationsDir.resolve("2020-01-01-paper2020.md")));
        assertTrue(Files.exists(publicationsDir.resolve("2021-01-01-book2021.md")));
    }

    @Test
    void journalTakesPriorityOverBooktitle(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("venue2020")
                .withField(StandardField.TITLE, "Venue Test")
                .withField(StandardField.JOURNAL, "Science")
                .withField(StandardField.BOOKTITLE, "Should Not Appear")
                .withField(StandardField.YEAR, "2020");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output").resolve("_publications").resolve("2020-01-01-venue2020.md"));
        assertTrue(content.contains("venue: 'Science'"));
        assertFalse(content.contains("Should Not Appear"));
    }

    @Test
    void missingAbstractOmitsBody(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("noabs2020")
                .withField(StandardField.TITLE, "No Abstract Paper")
                .withField(StandardField.YEAR, "2020");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output").resolve("_publications").resolve("2020-01-01-noabs2020.md"));
        assertTrue(content.endsWith("---\n"));
    }

    @Test
    void missingKeyUsesUnknown(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "No Key Paper")
                .withField(StandardField.YEAR, "2023");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        assertTrue(Files.exists(tempDir.resolve("output").resolve("_publications").resolve("2023-01-01-unknown.md")));
    }
}
