package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class CffExporterTest {

    private static Exporter cffExporter;
    private static BibDatabaseContext databaseContext;

    @BeforeAll
    static void setUp() {
        cffExporter = new TemplateExporter(
                "CFF",
                "cff",
                "cff",
                null,
                StandardFileType.CFF,
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                SaveOrder.getDefaultSaveOrder(),
                BlankLineBehaviour.DELETE_BLANKS);

        databaseContext = new BibDatabaseContext();
    }

    @Test
    public final void exportForNoEntriesWritesNothing(@TempDir Path tempFile) throws Exception {
        Path file = tempFile.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        cffExporter.export(databaseContext, tempFile, Collections.emptyList());
        assertEquals(Collections.emptyList(), Files.readAllLines(file));
    }

    @Test
    public final void exportsCorrectContent(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.URL, "http://example.com");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
        "cff-version: 1.2.0",
        "message: \"If you use this, please cite the work from preferred-citation.\"",
        "authors:",
        "  - name: Test Author",
        "title: Test Title",
        "preferred-citation:",
        "  type: article",
        "  authors:",
        "    - name: Test Author",
        "  title: Test Title",
        "  url: \"http://example.com\"");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void usesCorrectType(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.DOI, "random_doi_value");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: \"If you use this, please cite the work from preferred-citation.\"",
                "authors:",
                "  - name: Test Author",
                "title: Test Title",
                "preferred-citation:",
                "  type: conference-paper",
                "  authors:",
                "    - name: Test Author",
                "  title: Test Title",
                "  doi: random_doi_value");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void usesCorrectDefaultValues(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Thesis)
                .withCitationKey("test");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: \"If you use this, please cite the work from preferred-citation.\"",
                "authors:",
                "  - name: No author specified.",
                "title: No title specified.",
                "preferred-citation:",
                "  type: generic",
                "  authors:",
                "    - name: No author specified.",
                "  title: No title specified.");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    void passesModifiedCharset(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "谷崎 潤一郎")
                .withField(StandardField.TITLE, "細雪")
                .withField(StandardField.URL, "http://example.com");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        cffExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "cff-version: 1.2.0",
                "message: \"If you use this, please cite the work from preferred-citation.\"",
                "authors:",
                "  - name: 谷崎 潤一郎",
                "title: 細雪",
                "preferred-citation:",
                "  type: article",
                "  authors:",
                "    - name: 谷崎 潤一郎",
                "  title: 細雪",
                "  url: \"http://example.com\"");

        assertEquals(expected, Files.readAllLines(file));
    }
}
