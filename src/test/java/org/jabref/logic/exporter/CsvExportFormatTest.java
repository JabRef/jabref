package org.jabref.logic.exporter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(TempDirectory.class)
public class CsvExportFormatTest {

    public BibDatabaseContext databaseContext;
    public Charset charset;
    private Exporter exportFormat;

    @BeforeEach
    public void setUp() {
        Map<String, TemplateExporter> customFormats = new HashMap<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        ExporterFactory exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);

        exportFormat = exporterFactory.getExporterByName("oocsv").get();

        databaseContext = new BibDatabaseContext();
        charset = StandardCharsets.UTF_8;
    }

    @AfterEach
    public void tearDown() {
        exportFormat = null;
    }

    @Test
    public void testPerformExportForSingleAuthor(@TempDirectory.TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");

        BibEntry entry = new BibEntry();
        entry.setField("author", "Someone, Van Something");
        List<BibEntry> entries = Arrays.asList(entry);

        exportFormat.export(databaseContext, path, charset, entries);

        List<String> lines = Files.readAllLines(path);
        assertEquals(2, lines.size());
        assertEquals(
                "10,\"\",\"\",\"Someone, Van Something\",\"\",\"\",,,\"\",\"\",,\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"",
                lines.get(1));
    }

    @Test
    public void testPerformExportForMultipleAuthors(@TempDirectory.TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");

        BibEntry entry = new BibEntry();
        entry.setField("author", "von Neumann, John and Smith, John and Black Brown, Peter");
        List<BibEntry> entries = Arrays.asList(entry);

        exportFormat.export(databaseContext, path, charset, entries);

        List<String> lines = Files.readAllLines(path);
        assertEquals(2, lines.size());
        assertEquals(
                "10,\"\",\"\",\"von Neumann, John; Smith, John; Black Brown, Peter\",\"\",\"\",,,\"\",\"\",,\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"",
                lines.get(1));
    }

    @Test
    public void testPerformExportForSingleEditor(@TempDirectory.TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");
        File tmpFile = path.toFile();
        BibEntry entry = new BibEntry();
        entry.setField("editor", "Someone, Van Something");
        List<BibEntry> entries = Arrays.asList(entry);

        exportFormat.export(databaseContext, tmpFile.toPath(), charset, entries);

        List<String> lines = Files.readAllLines(tmpFile.toPath());
        assertEquals(2, lines.size());
        assertEquals(
                "10,\"\",\"\",\"\",\"\",\"\",,,\"\",\"\",,\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"Someone, Van Something\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"",
                lines.get(1));
    }

    @Test
    public void testPerformExportForMultipleEditors(@TempDirectory.TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");
        File tmpFile = path.toFile();
        BibEntry entry = new BibEntry();
        entry.setField("editor", "von Neumann, John and Smith, John and Black Brown, Peter");
        List<BibEntry> entries = Arrays.asList(entry);

        exportFormat.export(databaseContext, tmpFile.toPath(), charset, entries);

        List<String> lines = Files.readAllLines(tmpFile.toPath());
        assertEquals(2, lines.size());
        assertEquals(
                "10,\"\",\"\",\"\",\"\",\"\",,,\"\",\"\",,\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"von Neumann, John; Smith, John; Black Brown, Peter\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"",
                lines.get(1));
    }
}
