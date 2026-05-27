package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class GenericCsvExportFormatTest {

    public BibDatabaseContext databaseContext;
    private Exporter exportFormat;

    @BeforeEach
    void setUp() {
        exportFormat = new TemplateExporter(
                "CSV",
                "csv",
                "csv",
                "csv",
                StandardFileType.CSV,
                mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS),
                SaveOrder.getDefaultSaveOrder());

        databaseContext = new BibDatabaseContext();
    }

    @Test
    void performExportWithBasicFields(@TempDir Path testFolder) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path path = testFolder.resolve("test.csv");
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.TITLE, "Test Article")
                .withField(StandardField.YEAR, "2023");
        List<BibEntry> entries = List.of(entry);

        exportFormat.export(databaseContext, path, entries);

        List<String> lines = Files.readAllLines(path);
        assertEquals(2, lines.size()); // Header + 1 entry
        assertTrue(lines.get(0).contains("Citation Key"));
        assertTrue(lines.get(0).contains("Author"));
        assertTrue(lines.get(1).contains("Doe"));
    }

    @Test
    void performExportWithMultipleEntries(@TempDir Path testFolder) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path path = testFolder.resolve("test.csv");
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.TITLE, "Article 1")
                .withField(StandardField.YEAR, "2023");
        BibEntry entry2 = new BibEntry()
                .withField(StandardField.AUTHOR, "Smith, Jane")
                .withField(StandardField.TITLE, "Article 2")
                .withField(StandardField.YEAR, "2024");
        List<BibEntry> entries = List.of(entry1, entry2);

        exportFormat.export(databaseContext, path, entries);

        List<String> lines = Files.readAllLines(path);
        assertEquals(3, lines.size()); // Header + 2 entries
    }

    @Test
    void performExportWithEmptyEntryList(@TempDir Path testFolder) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path path = testFolder.resolve("test.csv");
        List<BibEntry> entries = List.of();

        exportFormat.export(databaseContext, path, entries);

        assertTrue(Files.notExists(path));
    }

    @Test
    void performExportContainsAllStandardFieldHeaders(@TempDir Path testFolder) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path path = testFolder.resolve("test.csv");
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, "Doe, John");
        List<BibEntry> entries = List.of(entry);

        exportFormat.export(databaseContext, path, entries);

        List<String> lines = Files.readAllLines(path);
        String header = lines.get(0);
        assertTrue(header.contains("Citation Key"));
        assertTrue(header.contains("Author"));
        assertTrue(header.contains("Title"));
        assertTrue(header.contains("Year"));
        assertTrue(header.contains("Journal"));
        assertTrue(header.contains("DOI"));
        assertTrue(header.contains("Abstract"));
    }
}
