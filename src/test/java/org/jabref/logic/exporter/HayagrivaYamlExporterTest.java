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

public class HayagrivaYamlExporterTest {

    private static Exporter hayagrivaYamlExporter;
    private static BibDatabaseContext databaseContext;

    @BeforeAll
    static void setUp() {
        hayagrivaYamlExporter = new TemplateExporter(
                "Hayagriva YAML",
                "hayagrivayaml",
                "hayagrivayaml",
                null,
                StandardFileType.YAML,
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                SaveOrder.getDefaultSaveOrder(),
                BlankLineBehaviour.DELETE_BLANKS);

        databaseContext = new BibDatabaseContext();
    }

    @Test
    public final void exportForNoEntriesWritesNothing(@TempDir Path tempFile) throws Exception {
        Path file = tempFile.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        hayagrivaYamlExporter.export(databaseContext, tempFile, Collections.emptyList());
        assertEquals(Collections.emptyList(), Files.readAllLines(file));
    }

    @Test
    public final void exportsCorrectContent(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.URL, "http://example.com")
                .withField(StandardField.DATE, "2020-10-14");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        hayagrivaYamlExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "test:",
                "  type: article",
                "  title: \"Test Title\"",
                "  author:",
                "    - Author, Test",
                "  date: 2020-10-14",
                "  url: http://example.com",
                "---");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void exportsCorrectMultipleAuthors(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author and Other One")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.URL, "http://example.com")
                .withField(StandardField.DATE, "2020-10-14");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        hayagrivaYamlExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "test:",
                "  type: article",
                "  title: \"Test Title\"",
                "  author:",
                "    - Author, Test",
                "    - One, Other",
                "  date: 2020-10-14",
                "  url: http://example.com",
                "---");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void formatsContentCorrect(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.URL, "http://example.com")
                .withField(StandardField.DATE, "2020-10-14");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        hayagrivaYamlExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "test:",
                "  type: Misc",
                "  title: \"Test Title\"",
                "  author:",
                "    - Author, Test",
                "  date: 2020-10-14",
                "  url: http://example.com",
                "---");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    void passesModifiedCharset(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "谷崎 潤一郎")
                .withField(StandardField.TITLE, "細雪")
                .withField(StandardField.URL, "http://example.com")
                .withField(StandardField.DATE, "2020-10-14");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        hayagrivaYamlExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "test:",
                "  type: article",
                "  title: \"細雪\"",
                "  author:",
                "    - 潤一郎, 谷崎",
                "  date: 2020-10-14",
                "  url: http://example.com",
                "---");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    void passesModifiedCharsetNull(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "谷崎 潤一郎")
                .withField(StandardField.TITLE, "細雪")
                .withField(StandardField.URL, "http://example.com")
                .withField(StandardField.DATE, "2020-10-14");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        hayagrivaYamlExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "test:",
                "  type: article",
                "  title: \"細雪\"",
                "  author:",
                "    - 潤一郎, 谷崎",
                "  date: 2020-10-14",
                "  url: http://example.com",
                "---");
        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    public final void exportsCorrectParentField(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test")
                .withField(StandardField.AUTHOR, "Test Author")
                .withField(StandardField.TITLE, "Test Title")
                .withField(StandardField.JOURNAL, "Test Publisher")
                .withField(StandardField.URL, "http://example.com")
                .withField(StandardField.DATE, "2020-10-14");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        hayagrivaYamlExporter.export(databaseContext, file, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "test:",
                "  type: article",
                "  title: \"Test Title\"",
                "  author:",
                "    - Author, Test",
                "  date: 2020-10-14",
                "  parent:",
                "    type: periodical",
                "    title: Test Publisher",
                "  url: http://example.com",
                "---");

        assertEquals(expected, Files.readAllLines(file));
    }
}
