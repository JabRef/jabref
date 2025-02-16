package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PdfXmpImporterTest {

    private PdfXmpImporter importer;

    @BeforeEach
    void setUp() {
        importer = new PdfXmpImporter(mock(XmpPreferences.class));
    }

    @Disabled("XMP reader prints warnings to the logger when parsing does not work")
    @Test
    void importEncryptedFileReturnsError() throws URISyntaxException {
        Path file = Path.of(PdfXmpImporterTest.class.getResource("/pdfs/encrypted.pdf").toURI());
        ParserResult result = importer.importDatabase(file);
        assertTrue(result.hasWarnings());
    }

    @Test
    void importEntries() throws URISyntaxException {
        Path file = Path.of(PdfXmpImporterTest.class.getResource("annotated.pdf").toURI());
        List<BibEntry> bibEntries = importer.importDatabase(file).getDatabase().getEntries();

        assertEquals(1, bibEntries.size());

        BibEntry be0 = bibEntries.getFirst();
        assertEquals(Optional.of("how to annotate a pdf"), be0.getField(StandardField.ABSTRACT));
        assertEquals(Optional.of("Chris"), be0.getField(StandardField.AUTHOR));
        assertEquals(Optional.of("pdf, annotation"), be0.getField(StandardField.KEYWORDS));
        assertEquals(Optional.of("The best Pdf ever"), be0.getField(StandardField.TITLE));
    }

    @Test
    void isRecognizedFormat() throws IOException, URISyntaxException {
        Path file = Path.of(PdfXmpImporterTest.class.getResource("annotated.pdf").toURI());
        assertTrue(importer.isRecognizedFormat(file));
    }
}
