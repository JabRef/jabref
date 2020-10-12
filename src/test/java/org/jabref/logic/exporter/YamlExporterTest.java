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
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.withCitationKey("test");
        entry.withField(StandardField.AUTHOR, "Test Author");
        entry.withField(StandardField.TITLE, "Test Title");
        entry.withField(StandardField.URL, "http://example.com");
        entry.withField(StandardField.YEAR, "2020");

        Path file = tempFile.resolve("RandomFileName");
        Files.createFile(file);
        yamlExporter.export(databaseContext, file, charset, Collections.singletonList(entry));

        List<String> lines = new ArrayList<>();
        lines.add("---");
        lines.add("references:");
        lines.add("- id: test");
        lines.add("  author:");
        lines.add("  - literal: \"Test Author\"");
        lines.add("  title: \"Test Title\"");
        lines.add("  issued: 2020");
        lines.add("  url: http://example.com");
        lines.add("---");

        assertEquals(lines, Files.readAllLines(file));
    }
}
