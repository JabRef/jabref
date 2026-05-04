package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AcademicPagesExporterTest {

    private AcademicPagesExporter exporter;
    private BibDatabaseContext databaseContext;
    private BibEntryTypesManager entryTypesManager;

    @BeforeEach
    void setUp() {
        entryTypesManager = new BibEntryTypesManager();
        exporter = new AcademicPagesExporter(
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                mock(FieldPreferences.class, Answers.RETURNS_DEEP_STUBS),
                entryTypesManager);
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
                .withField(StandardField.JOURNAL, "Nature")
                .withField(StandardField.YEAR, "2020");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        Path generated = tempDir.resolve("output")
                                .resolve("_publications")
                                .resolve("2020-01-01-smith2020.md");
        assertTrue(Files.exists(generated));

        String content = Files.readString(generated).replace("\r\n", "\n");
        String citation = extractYamlValue(content, "citation");

        String expected = """
                ---
                title: "A Great Paper"
                collection: publications
                venue: "Nature"
                category: manuscripts
                permalink: /publication/2020-01-01-smith2020
                date: 2020-01-01
                bibtexurl: '/files/smith2020.bib'
                citation: '%s'
                ---
                """.formatted(citation);

        assertEquals(expected, content);
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
    void inProceedingsIsConferences(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("jones2021conf")
                .withField(StandardField.TITLE, "Conference Paper")
                .withField(StandardField.AUTHOR, "Jones, Bob")
                .withField(StandardField.BOOKTITLE, "Proceedings of ICSE")
                .withField(StandardField.YEAR, "2021");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        Path generated = tempDir.resolve("output")
                                .resolve("_publications")
                                .resolve("2021-01-01-jones2021conf.md");
        assertTrue(Files.exists(generated));

        String content = Files.readString(generated).replace("\r\n", "\n");
        String citation = extractYamlValue(content, "citation");

        String expected = """
                ---
                title: "Conference Paper"
                collection: publications
                venue: "Proceedings of ICSE"
                category: conferences
                permalink: /publication/2021-01-01-jones2021conf
                date: 2021-01-01
                bibtexurl: '/files/jones2021conf.bib'
                citation: '%s'
                ---
                """.formatted(citation);

        assertEquals(expected, content);
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

        Path generated = tempDir.resolve("output")
                                .resolve("_publications")
                                .resolve("2019-01-01-doe2019book.md");
        assertTrue(Files.exists(generated));

        String content = Files.readString(generated).replace("\r\n", "\n");
        String citation = extractYamlValue(content, "citation");

        String expected = """
                ---
                title: "My Book"
                collection: publications
                category: manuscripts
                permalink: /publication/2019-01-01-doe2019book
                date: 2019-01-01
                bibtexurl: '/files/doe2019book.bib'
                citation: '%s'
                ---
                """.formatted(citation);

        assertEquals(expected, content);
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

        Path generated = tempDir.resolve("output")
                                .resolve("_publications")
                                .resolve("2018-01-01-lee2018chapter.md");
        assertTrue(Files.exists(generated));

        String content = Files.readString(generated).replace("\r\n", "\n");
        String citation = extractYamlValue(content, "citation");

        String expected = """
                ---
                title: "A Book Chapter"
                collection: publications
                venue: "Handbook of AI"
                category: manuscripts
                permalink: /publication/2018-01-01-lee2018chapter
                date: 2018-01-01
                bibtexurl: '/files/lee2018chapter.bib'
                citation: '%s'
                ---
                """.formatted(citation);

        assertEquals(expected, content);
    }

    @Test
    void miscIsManuscripts(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("misc2022")
                .withField(StandardField.TITLE, "Some Misc Entry")
                .withField(StandardField.YEAR, "2022");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        Path generated = tempDir.resolve("output")
                                .resolve("_publications")
                                .resolve("2022-01-01-misc2022.md");
        assertTrue(Files.exists(generated));

        String content = Files.readString(generated).replace("\r\n", "\n");
        String citation = extractYamlValue(content, "citation");

        String expected = """
                ---
                title: "Some Misc Entry"
                collection: publications
                category: manuscripts
                permalink: /publication/2022-01-01-misc2022
                date: 2022-01-01
                bibtexurl: '/files/misc2022.bib'
                citation: '%s'
                ---
                """.formatted(citation);

        assertEquals(expected, content);
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

        String content = Files.readString(tempDir.resolve("output")
                                                 .resolve("_publications")
                                                 .resolve("2020-01-01-venue2020.md")).replace("\r\n", "\n");
        assertTrue(content.contains("venue: \"Science\""));
        assertFalse(content.contains("Should Not Appear"));
    }

    @Test
    void missingAbstractOmitsBody(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("noabs2020")
                .withField(StandardField.TITLE, "No Abstract Paper")
                .withField(StandardField.YEAR, "2020");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output")
                                                 .resolve("_publications")
                                                 .resolve("2020-01-01-noabs2020.md")).replace("\r\n", "\n");
        assertTrue(content.endsWith("---\n"));
    }

    @Test
    void missingKeyUsesUnknown(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "No Key Paper")
                .withField(StandardField.YEAR, "2023");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        assertTrue(Files.exists(tempDir.resolve("output")
                                       .resolve("_publications")
                                       .resolve("2023-01-01-unknown.md")));
    }

    @Test
    void createsBothSubdirectories(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("smith2020")
                .withField(StandardField.TITLE, "A Paper")
                .withField(StandardField.YEAR, "2020");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        assertTrue(Files.exists(tempDir.resolve("output").resolve("_publications")));
        assertTrue(Files.exists(tempDir.resolve("output").resolve("files")));
    }

    @Test
    void abstractAppearsAfterFrontMatter(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("abs2020")
                .withField(StandardField.TITLE, "Abstract Test")
                .withField(StandardField.ABSTRACT, "This is my abstract.")
                .withField(StandardField.YEAR, "2020");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output")
                                                 .resolve("_publications")
                                                 .resolve("2020-01-01-abs2020.md")).replace("\r\n", "\n");

        String[] parts = content.split("---\n");
        // parts[0] is empty (before first ---), parts[1] is YAML, parts[2] is body
        assertEquals(3, parts.length);
        assertTrue(parts[2].strip().contains("This is my abstract."));
    }

    @Test
    void missingDateOmitsDateAndPermalink(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("nodate")
                .withField(StandardField.TITLE, "No Date Paper");

        exporter.export(databaseContext, tempDir.resolve("output.md"), List.of(entry));

        String content = Files.readString(tempDir.resolve("output")
                                                 .resolve("_publications")
                                                 .resolve("nodate.md")).replace("\r\n", "\n");
        assertFalse(content.contains("date:"));
        assertFalse(content.contains("permalink:"));
    }

    private String extractYamlValue(String content, String key) {
        for (String line : content.lines().toList()) {
            if (line.startsWith(key + ":")) {
                String value = line.substring(key.length() + 1).strip();
                if ((value.startsWith("'") && value.endsWith("'")) ||
                        (value.startsWith("\"") && value.endsWith("\""))) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return "";
    }
}
