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
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class YamlExporterTest {

    public Charset charset;
    private Exporter yamlExporter;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    public void setUp() throws Exception {
        List<TemplateExporter> customFormats = new ArrayList<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);

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
    public final void exportCorrectContent(@TempDir Path tempFile) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
          .withCitationKey("test")
          .withField(StandardField.AUTHOR, "Test Author")
          .withField(StandardField.TITLE, "Test Title")
          .withField(StandardField.URL, "http://example.com")
          .withField(StandardField.YEAR, "2020");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        yamlExporter.export(databaseContext, file, charset, Collections.singletonList(entry));

        List<String> lines = List.of(
          "---",
         "references:",
         "- id: test",
         "  author:",
         "  - literal: \"Test Author\"",
         "  title: \"Test Title\"",
         "  issued: 2020",
         "  url: http://example.com",
         "---");

        assertEquals(lines, Files.readAllLines(file));
    }
}
