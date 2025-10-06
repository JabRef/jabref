package org.jabref.logic.importer.fileformat.pdf;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.xmp.EncryptedPdfsNotSupportedException;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.logic.util.StandardFileType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PdfImporter using mocks.
 */
public class PdfImporterTest {

    static class TestPdfImporter extends PdfImporter {

        private final XmpUtilReader reader;

        TestPdfImporter(XmpUtilReader reader) {
            this.reader = reader;
        }

        @Override
        public String getId() {
            return "TestPdfImporter";
        }

        @Override
        public String getName() {
            return "Test PDF Importer";
        }

        @Override
        public String getDescription() {
            return "Mock PdfImporter for testing";
        }

        @Override
        public StandardFileType getFileType() {
            return StandardFileType.PDF;
        }

        @Override
        public List<BibEntry> importDatabase(Path filePath, PDDocument document) throws IOException, ParseException {
            BibEntry entry = new BibEntry();
            entry.setField(StandardField.TITLE, "Mock PDF Entry");
            return List.of(entry);
        }
    }

    @Test
    void testSuccessfulImportDatabase() throws Exception {
        XmpUtilReader reader = mock(XmpUtilReader.class);
        PDDocument doc = mock(PDDocument.class);
        Path filePath = Path.of("test.pdf");

        when(reader.loadWithAutomaticDecryption(filePath)).thenReturn(doc);

        PdfImporter importer = new TestPdfImporter(reader);
        ParserResult result = importer.importDatabase(filePath);

        assertNotNull(result);
        assertTrue(result.getDatabaseContext().getDatabase().hasEntries());
    }

    @Test
    void testEncryptedPdfHandling() throws Exception {
        XmpUtilReader reader = mock(XmpUtilReader.class);
        Path filePath = Path.of("encrypted.pdf");

        when(reader.loadWithAutomaticDecryption(filePath))
                .thenThrow(new EncryptedPdfsNotSupportedException());

        PdfImporter importer = new TestPdfImporter(reader);
        ParserResult result = importer.importDatabase(filePath);

        assertTrue(result.isInvalid() || result.hasWarnings(), "Should mark result invalid for encrypted PDF");
    }

    @Test
    void testIOExceptionHandling() throws Exception {
        XmpUtilReader reader = mock(XmpUtilReader.class);
        Path filePath = Path.of("bad.pdf");

        when(reader.loadWithAutomaticDecryption(filePath)).thenThrow(new IOException("IO problem"));

        PdfImporter importer = new TestPdfImporter(reader);
        ParserResult result = importer.importDatabase(filePath);

        assertTrue(result.isInvalid() || result.hasWarnings());
    }
}
