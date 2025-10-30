package org.jabref.logic.importer.fileformat.pdf;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PdfImporterMockTest {

    @Test
    public void testImportDatabase_handlesIOException() {
        PdfImporter importer = new PdfImporter() {
            @Override
            public String getId() {
                return "mock-pdf-importer";
            }

            @Override
            public String getName() {
                return "Mock PDF Importer";
            }

            @Override
            public String getDescription() {
                return "A mock importer used for testing IOException handling.";
            }

            @Override
            public List<BibEntry> importDatabase(Path filePath, PDDocument document)
                    throws IOException, ParseException {
                throw new IOException("Simulated read error");
            }
        };

        ParserResult result = importer.importDatabase(Path.of("fake.pdf"));
        assertNotNull(result, "ParserResult should not be null even on failure");
        assertTrue(result.getErrorMessage() != null && !result.getErrorMessage().isEmpty(),
                "Expected an error message on simulated read error");
    }

    @Test
    public void testImportDatabase_returnsEntries() throws Exception {
        PdfImporter importer = new PdfImporter() {
            @Override
            public String getId() {
                return "mock-pdf-importer";
            }

            @Override
            public String getName() {
                return "Mock PDF Importer";
            }

            @Override
            public String getDescription() {
                return "A mock importer returning one BibEntry.";
            }

            @Override
            public List<BibEntry> importDatabase(Path filePath, PDDocument document) {
                return List.of(new BibEntry());
            }
        };

        ParserResult result = importer.importDatabase(Path.of("sample.pdf"));
        assertNotNull(result);
        assertEquals(1, result.getDatabaseContext().getEntries().size(),
                "Expected one BibEntry from mock importer");
    }
}
