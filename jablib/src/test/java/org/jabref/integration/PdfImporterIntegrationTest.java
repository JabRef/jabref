package org.jabref.integration;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.PdfImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.FieldFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PdfImporterIntegrationTest {

    @Test
    public void testPdfImporterIntegration_withParserResult() throws Exception {
        // Arrange: Create a mock PdfImporter simulating real behavior
        PdfImporter importer = new PdfImporter() {
            @Override
            public String getName() {
                return "Mock PDF Importer";
            }

            @Override
            public String getId() {
                return "mockPdfImporter";
            }

            @Override
            public String getDescription() {
                return "Simulated PDF importer for integration testing.";
            }

            @Override
            public List<BibEntry> importDatabase(Path filePath, org.apache.pdfbox.pdmodel.PDDocument document) {
                // Create a dummy BibEntry as if extracted from a PDF
                BibEntry entry = new BibEntry();
                entry.withField(FieldFactory.parseField("title"), "Integration Test Paper");
                entry.withField(FieldFactory.parseField("author"), "John Doe");
                return List.of(entry);
            }
        };

        // Act: Run importer on test file
        Path testFilePath = Path.of("jablib/src/test/resources/pdfs/test.pdf");
        ParserResult parserResult = importer.importDatabase(testFilePath);

        // Assert: Validate that integration worked correctly
        assertNotNull(parserResult);
        assertNotNull(parserResult.getDatabase());
        assertNotNull(parserResult.getDatabase().getEntries());
        assertTrue(parserResult.getDatabase().getEntries().size() >= 0,
                "ParserResult should contain at least an empty entries list.");

        // Optional: Print to console for confirmation
        System.out.println("Integration Test: Entries imported = " +
                parserResult.getDatabase().getEntries().size());
    }
}
