package org.jabref.logic.exporter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class YamlExporterTest {

    private static Charset charset;
    private static Exporter yamlExporter;
    private static BibDatabaseContext databaseContext;

    @BeforeAll
    static void setUp() {
        List<TemplateExporter> customFormats = new ArrayList<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences, BibDatabaseMode.BIBTEX, entryTypesManager);

        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
        yamlExporter = exporterFactory.getExporterByName("yaml").get();
    }

    @Test
    public final void exportForNoEntriesWritesNothing(@TempDir Path tempFile) throws Exception {
        Path file = tempFile.resolve("ThisIsARandomlyNamedFile");
        Files.createFile(file);
        yamlExporter.export(databaseContext, tempFile, charset, Collections.emptyList());
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
        yamlExporter.export(databaseContext, file, charset, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "references:",
                "- id: test",
                "  type: article",
                "  author:",
                "  - literal: \"Test Author\"",
                "  title: \"Test Title\"",
                "  issued: 2020-10-14",
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
        yamlExporter.export(databaseContext, file, charset, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "references:",
                "- id: test",
                "  type: no-type",
                "  author:",
                "  - literal: \"Test Author\"",
                "  title: \"Test Title\"",
                "  issued: 2020-10-14",
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
        yamlExporter.export(databaseContext, file, StandardCharsets.UTF_8, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "references:",
                "- id: test",
                "  type: article",
                "  author:",
                "  - literal: \"谷崎 潤一郎\"",
                "  title: \"細雪\"",
                "  issued: 2020-10-14",
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
        yamlExporter.export(databaseContext, file, null, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "references:",
                "- id: test",
                "  type: article",
                "  author:",
                "  - literal: \"谷崎 潤一郎\"",
                "  title: \"細雪\"",
                "  issued: 2020-10-14",
                "  url: http://example.com",
                "---");

        assertEquals(expected, Files.readAllLines(file));
    }

    @Test
    void passesModifiedCharsetASCII(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
            .withCitationKey("test")
            .withField(StandardField.AUTHOR, "谷崎 潤一郎")
            .withField(StandardField.TITLE, "細雪")
            .withField(StandardField.URL, "http://example.com")
            .withField(StandardField.DATE, "2020-10-14");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        yamlExporter.export(databaseContext, file, StandardCharsets.US_ASCII, Collections.singletonList(entry));

        List<String> expected = List.of(
                "---",
                "references:",
                "- id: test",
                "  type: article",
                "  author:",
                "  - literal: \"?? ???\"",
                "  title: \"??\"",
                "  issued: 2020-10-14",
                "  url: http://example.com",
                "---");

        assertEquals(expected, Files.readAllLines(file));

    }
}
