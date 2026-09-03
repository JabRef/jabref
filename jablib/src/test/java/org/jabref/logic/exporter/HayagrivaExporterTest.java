package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HayagrivaExporterTest {

    private final HayagrivaExporter exporter = new HayagrivaExporter();
    private final BibDatabaseContext databaseContext = new BibDatabaseContext();

    private List<String> export(List<BibEntry> entries, Path tempDir) throws IOException {
        Path file = tempDir.resolve("export.yml");
        Files.createFile(file);
        exporter.export(databaseContext, file, entries);
        return Files.readAllLines(file);
    }

    @Test
    void exportForNoEntriesWritesNothing(@TempDir Path tempDir) throws IOException {
        assertEquals(List.of(), export(List.of(), tempDir));
    }

    @Test
    void exportsCorrectContent(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Author, Test")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.URL, "http://example.com")
                .withField(StandardField.DATE, "2020-10-14");

        List<String> expected = List.of(
                "test:",
                "  type: article",
                "  title: Test Title",
                "  author:",
                "  - \"Author, Test\"",
                "  date: 2020-10-14",
                "  url: http://example.com");

        assertEquals(expected, export(List.of(entry), tempDir));
    }

    @Test
    void exportsCorrectMultipleAuthors(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Author, Test and One, Other")
                .withField(StandardField.TITLE, "Test Title");

        List<String> expected = List.of(
                "test:",
                "  type: article",
                "  title: Test Title",
                "  author:",
                "  - \"Author, Test\"",
                "  - \"One, Other\"");

        assertEquals(expected, export(List.of(entry), tempDir));
    }

    @Test
    void unmappedTypeExportsAsLowercaseMisc(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Dataset)
                .withCitationKey("test")
                .withField(StandardField.TITLE, "Test Title");

        List<String> expected = List.of(
                "test:",
                "  type: misc",
                "  title: Test Title");

        assertEquals(expected, export(List.of(entry), tempDir));
    }

    @Test
    void exportsNonAsciiContentVerbatim(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "潤一郎, 谷崎")
                .withField(StandardField.TITLE, "細雪");

        List<String> expected = List.of(
                "test:",
                "  type: article",
                "  title: 細雪",
                "  author:",
                "  - \"潤一郎, 谷崎\"");

        assertEquals(expected, export(List.of(entry), tempDir));
    }

    @Test
    void journalBecomesPeriodicalParentCarryingVolumeIssueAndPublisher(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.JOURNAL, "Physical Review B")
                .withField(StandardField.VOLUME, "102")
                .withField(StandardField.NUMBER, "16")
                .withField(StandardField.PUBLISHER, "American Physical Society");

        List<String> expected = List.of(
                "test:",
                "  type: article",
                "  title: Test Title",
                "  parent:",
                "    type: periodical",
                "    title: Physical Review B",
                "    volume: 102",
                "    issue: 16",
                "    publisher: American Physical Society");

        assertEquals(expected, export(List.of(entry), tempDir));
    }

    @Test
    void booktitleBecomesProceedingsParentForInProceedings(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("test")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.BOOKTITLE, "Proceedings of Testing");

        List<String> expected = List.of(
                "test:",
                "  type: conference",
                "  title: Test Title",
                "  parent:",
                "    type: proceedings",
                "    title: Proceedings of Testing");

        assertEquals(expected, export(List.of(entry), tempDir));
    }

    @Test
    void seriesBecomesBookParent(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("test")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.SERIES, "Annals of Mathematics Studies");

        List<String> expected = List.of(
                "test:",
                "  type: book",
                "  title: Test Title",
                "  parent:",
                "    type: book",
                "    title: Annals of Mathematics Studies");

        assertEquals(expected, export(List.of(entry), tempDir));
    }

    @Test
    void exportsMultipleEntriesAsSingleYamlDocument(@TempDir Path tempDir) throws IOException {
        BibEntry first = new BibEntry(StandardEntryType.Article)
                .withCitationKey("first")
                .withField(StandardField.TITLE, "First Title");
        BibEntry second = new BibEntry(StandardEntryType.Article)
                .withCitationKey("second")
                .withField(StandardField.TITLE, "Second Title");

        List<String> expected = List.of(
                "first:",
                "  type: article",
                "  title: First Title",
                "second:",
                "  type: article",
                "  title: Second Title");

        assertEquals(expected, export(List.of(first, second), tempDir));
    }

    @Test
    void exportsIdentifiersNestedUnderSerialNumber(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DOI, "10.1109/EDOC.2018.00030")
                .withField(StandardField.ISSN, "0896-3207");

        List<String> expected = List.of(
                "test:",
                "  type: article",
                "  title: Test Title",
                "  serial-number:",
                "    doi: 10.1109/EDOC.2018.00030",
                "    issn: 0896-3207");

        assertEquals(expected, export(List.of(entry), tempDir));
    }

    @Test
    void exportsAffiliatedRoles(@TempDir Path tempDir) throws IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("test")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.TRANSLATOR, "Translator, Some")
                .withField(new UnknownField("director"), "Director, Other");

        List<String> expected = List.of(
                "test:",
                "  type: misc",
                "  title: Test Title",
                "  affiliated:",
                "  - role: translator",
                "    names:",
                "    - \"Translator, Some\"",
                "  - role: director",
                "    names:",
                "    - \"Director, Other\"");

        assertEquals(expected, export(List.of(entry), tempDir));
    }

    @Test
    void missingAndDuplicateCitationKeysGetUniqueKeys(@TempDir Path tempDir) throws IOException {
        BibEntry unkeyed = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, "No Key");
        BibEntry first = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("dup")
                .withField(StandardField.TITLE, "First");
        BibEntry second = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("dup")
                .withField(StandardField.TITLE, "Second");

        List<String> expected = List.of(
                "entry-1:",
                "  type: misc",
                "  title: No Key",
                "dup:",
                "  type: misc",
                "  title: First",
                "dup-1:",
                "  type: misc",
                "  title: Second");

        assertEquals(expected, export(List.of(unkeyed, first, second), tempDir));
    }
}
