package org.jabref.logic.exporter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CsvExportFormatTest {

    public BibDatabaseContext databaseContext;
    private Exporter exportFormat;

    @BeforeEach
    public void setUp() {
        SaveConfiguration saveConfiguration = mock(SaveConfiguration.class);
        when(saveConfiguration.getSaveOrder()).thenReturn(SaveOrder.getDefaultSaveOrder());

        exportFormat = new TemplateExporter(
                "OpenOffice/LibreOffice CSV",
                "oocsv",
                "openoffice-csv",
                "openoffice",
                StandardFileType.CSV,
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                saveConfiguration);

        databaseContext = new BibDatabaseContext();
    }

    @AfterEach
    public void tearDown() {
        exportFormat = null;
    }

    @Test
    public void testPerformExportForSingleAuthor(@TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");

        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Someone, Van Something");
        List<BibEntry> entries = List.of(entry);

        exportFormat.export(databaseContext, path, entries);

        List<String> lines = Files.readAllLines(path);
        assertEquals(2, lines.size());
        assertEquals(
                "10,\"\",\"\",\"Someone, Van Something\",\"\",\"\",,,\"\",\"\",,\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"",
                lines.get(1));
    }

    @Test
    public void testPerformExportForMultipleAuthors(@TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");

        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "von Neumann, John and Smith, John and Black Brown, Peter");
        List<BibEntry> entries = List.of(entry);

        exportFormat.export(databaseContext, path, entries);

        List<String> lines = Files.readAllLines(path);
        assertEquals(2, lines.size());
        assertEquals(
                "10,\"\",\"\",\"von Neumann, John; Smith, John; Black Brown, Peter\",\"\",\"\",,,\"\",\"\",,\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"",
                lines.get(1));
    }

    @Test
    public void testPerformExportForSingleEditor(@TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");
        File tmpFile = path.toFile();
        BibEntry entry = new BibEntry().withField(StandardField.EDITOR, "Someone, Van Something");
        List<BibEntry> entries = List.of(entry);

        exportFormat.export(databaseContext, tmpFile.toPath(), entries);

        List<String> lines = Files.readAllLines(tmpFile.toPath());
        assertEquals(2, lines.size());
        assertEquals(
                "10,\"\",\"\",\"\",\"\",\"\",,,\"\",\"\",,\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"Someone, Van Something\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"",
                lines.get(1));
    }

    @Test
    public void testPerformExportForMultipleEditors(@TempDir Path testFolder) throws Exception {
        Path path = testFolder.resolve("ThisIsARandomlyNamedFile");
        File tmpFile = path.toFile();
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.EDITOR, "von Neumann, John and Smith, John and Black Brown, Peter");
        List<BibEntry> entries = List.of(entry);

        exportFormat.export(databaseContext, tmpFile.toPath(), entries);

        List<String> lines = Files.readAllLines(tmpFile.toPath());
        assertEquals(2, lines.size());
        assertEquals(
                "10,\"\",\"\",\"\",\"\",\"\",,,\"\",\"\",,\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"von Neumann, John; Smith, John; Black Brown, Peter\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\"",
                lines.get(1));
    }
}
