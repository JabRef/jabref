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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("exporter")
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

    @AfterEach
    void tearDown() {
        exportFormat = null;
    }

    @Test
    void performExportForSingleAuthor(@TempDir Path testFolder) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path path = testFolder.resolve("test.csv");
        BibEntry entry = new BibEntry()
                .withCitationKey("Doe2023")
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.TITLE, "Test Article")
                .withField(StandardField.YEAR, "2023");
        List<BibEntry> entries = List.of(entry);

        exportFormat.export(databaseContext, path, entries);

        List<String> lines = Files.readAllLines(path);
        assertEquals(2, lines.size());
        assertEquals(
                """
                        "Citation Key","Author","Title","Year","Journal","Booktitle","Publisher","Volume","Number","Pages","Month","Edition","Address","Editor","Series","Note","HowPublished","Organization","Institution","School","Chapter","Annote","DOI","URL","Keywords","Abstract","ISBN","ISSN"\
                        """,
                lines.getFirst());
        assertEquals(
                """
                        "Doe2023","Doe, John","Test Article","2023","","","","","","","","","","","","","","","","","","","","","","","",""\
                        """,
                lines.get(1));
    }

    @Test
    void performExportForMultipleEntries(@TempDir Path testFolder) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path path = testFolder.resolve("test.csv");
        BibEntry entry1 = new BibEntry()
                .withCitationKey("Doe2023")
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.TITLE, "Article 1")
                .withField(StandardField.YEAR, "2023");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("Smith2024")
                .withField(StandardField.AUTHOR, "Smith, Jane")
                .withField(StandardField.TITLE, "Article 2")
                .withField(StandardField.YEAR, "2024");
        List<BibEntry> entries = List.of(entry1, entry2);

        exportFormat.export(databaseContext, path, entries);

        List<String> lines = Files.readAllLines(path);
        assertEquals(3, lines.size());
    }

    @Test
    void performExportEscapesDoubleQuotesInFields(@TempDir Path testFolder) throws IOException, SaveException, ParserConfigurationException, TransformerException {
        Path path = testFolder.resolve("test.csv");
        BibEntry entry = new BibEntry()
                .withCitationKey("Doe2023")
                .withField(StandardField.TITLE, "Title with \"quotes\" inside");
        List<BibEntry> entries = List.of(entry);

        exportFormat.export(databaseContext, path, entries);

        List<String> lines = Files.readAllLines(path);
        assertEquals(2, lines.size());
        assertEquals(
                """
                        "Doe2023","","Title with ""quotes"" inside","","","","","","","","","","","","","","","","","","","","","","","","",""\
                        """,
                lines.get(1));
    }
}
